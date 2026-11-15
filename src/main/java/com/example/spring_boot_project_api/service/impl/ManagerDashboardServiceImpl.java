package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.ManagerDashboardResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.MaintenanceRequest;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.MaintenanceRequestRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.util.InvoiceBalanceCalculator;
import com.example.spring_boot_project_api.service.ManagerDashboardService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManagerDashboardServiceImpl implements ManagerDashboardService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final RoomRepository roomRepository;
    private final RentalContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional(readOnly = true)
    public ManagerDashboardResponse getDashboard(Long houseId, LocalDate fromDate, LocalDate toDate,
            RoomStatus roomStatus, InvoiceStatus paymentStatus) {
        List<Long> scopeHouseIds = resolveScopeHouseIds(houseId);

        if (scopeHouseIds.isEmpty()) {
            return emptyDashboard();
        }

        // Room metrics (respect optional room-status filter via per-status counts).
        Map<RoomStatus, Long> rawRoomStatusCounts = roomCountsByStatus(scopeHouseIds);
        Map<RoomStatus, Long> roomStatusCounts = new EnumMap<>(RoomStatus.class);
        if (roomStatus != null) {
            roomStatusCounts.put(roomStatus, rawRoomStatusCounts.getOrDefault(roomStatus, 0L));
        } else {
            roomStatusCounts.putAll(rawRoomStatusCounts);
        }
        long totalRooms = roomStatusCounts.values().stream().mapToLong(Long::longValue).sum();
        long availableRooms = roomStatusCounts.getOrDefault(RoomStatus.AVAILABLE, 0L);
        long occupiedRooms = roomStatusCounts.getOrDefault(RoomStatus.OCCUPIED, 0L);
        long maintenanceRooms = roomStatusCounts.getOrDefault(RoomStatus.MAINTENANCE, 0L);

        Map<RoomStatus, Long> normalizedRoomCounts = new EnumMap<>(RoomStatus.class);
        normalizedRoomCounts.putAll(roomStatusCounts);
        KNOWN_ROOM_STATUSES.stream()
                .filter(status -> !normalizedRoomCounts.containsKey(status))
                .forEach(status -> normalizedRoomCounts.put(status, 0L));

        // Contracts + invoices scoped to the houses.
        List<Long> contractIds = contractRepository.findByRoomHouseIdIn(scopeHouseIds).stream()
                .map(RentalContract::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Invoice> scopeInvoices = contractIds.isEmpty()
                ? List.of()
                : invoiceRepository.findByContractIdIn(contractIds);

        // The payment-status donut counts invoices created within the date range;
        // revenue counts payments actually received within the date range.
        List<Invoice> rangeInvoices = filterInvoicesByDate(scopeInvoices, fromDate, toDate);

        // Payment metrics: revenue (paid amount) + payment status counts.
        Map<InvoiceStatus, Long> invoiceStatusCounts = invoiceStatusCounts(rangeInvoices, paymentStatus);
        long pendingInvoices = invoiceStatusCounts.getOrDefault(InvoiceStatus.PENDING, 0L);
        // Derived from due date + outstanding balance. The invoice lifecycle never persists
        // InvoiceStatus.OVERDUE, so counting that status always reported zero. Date scope is
        // unchanged: still the invoices created within the selected range.
        long overdueInvoices = countOverdueInvoices(rangeInvoices);

        BigDecimal revenue = sumRevenue(scopeInvoices, fromDate, toDate);
        double occupancyRate = totalRooms == 0 ? 0.0
                : BigDecimal.valueOf(occupiedRooms)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalRooms), 1, RoundingMode.HALF_UP)
                        .doubleValue();

        // Revenue trend by month.
        List<ManagerDashboardResponse.RevenuePoint> revenueTrend =
                buildRevenueTrend(scopeInvoices, fromDate, toDate);

        // Room occupancy grouped by house.
        List<ManagerDashboardResponse.RoomOccupancyPoint> roomOccupancy =
                buildRoomOccupancy(scopeHouseIds, roomStatus, roomStatusCounts);

        // Maintenance status breakdown.
        List<ManagerDashboardResponse.StatusCount> maintenanceStatus =
                buildMaintenanceStatus(scopeHouseIds, fromDate, toDate);

        // Payment status breakdown (invoice statuses within the filtered scope).
        List<ManagerDashboardResponse.StatusCount> paymentStatusBreakdown =
                invoiceStatusCounts.entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getKey().name()))
                        .map(e -> new ManagerDashboardResponse.StatusCount(e.getKey().name(), e.getValue()))
                        .toList();

        return new ManagerDashboardResponse(
                new ManagerDashboardResponse.Kpis(
                        totalRooms,
                        availableRooms,
                        occupiedRooms,
                        maintenanceRooms,
                        revenue,
                        occupancyRate,
                        pendingInvoices,
                        overdueInvoices),
                revenueTrend,
                roomOccupancy,
                paymentStatusBreakdown,
                maintenanceStatus);
    }

    private List<Long> resolveScopeHouseIds(Long houseId) {
        List<Long> accessible = houseAccessService.getAccessibleHouseIds();
        if (houseId == null) {
            return accessible;
        }
        // RequireHouseAccess throws AccessDeniedException if not accessible.
        houseAccessService.requireHouseAccess(houseId);
        return List.of(houseId);
    }

    private ManagerDashboardResponse emptyDashboard() {
        return new ManagerDashboardResponse(
                new ManagerDashboardResponse.Kpis(0, 0, 0, 0, BigDecimal.ZERO, 0.0, 0, 0),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private Map<RoomStatus, Long> roomCountsByStatus(List<Long> scopeHouseIds) {
        Map<RoomStatus, Long> counts = new EnumMap<>(RoomStatus.class);
        for (RoomStatus status : RoomStatus.values()) {
            long count = roomRepository.countByHouseIdInAndStatus(scopeHouseIds, status);
            if (count > 0) {
                counts.put(status, count);
            }
        }
        return counts;
    }

    /**
     * Counts invoices whose due date has passed and that still owe money. PAID payment
     * totals are fetched in one grouped query, so this adds no per-invoice payment lookup.
     */
    private long countOverdueInvoices(List<Invoice> invoices) {
        if (invoices.isEmpty()) {
            return 0L;
        }
        List<Long> invoiceIds = invoices.stream()
                .map(Invoice::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, BigDecimal> paidByInvoice = new HashMap<>();
        List<Object[]> rows = invoiceIds.isEmpty()
                ? List.of()
                : paymentRepository.sumPaidAmountByInvoiceIds(invoiceIds, PaymentStatus.PAID);
        if (rows != null) {
            for (Object[] row : rows) {
                if (row == null || row.length < 2 || row[0] == null) {
                    continue;
                }
                BigDecimal amount = row[1] instanceof BigDecimal value
                        ? value : new BigDecimal(row[1].toString());
                paidByInvoice.put(((Number) row[0]).longValue(), amount);
            }
        }

        LocalDate today = LocalDate.now();
        long count = 0L;
        for (Invoice invoice : invoices) {
            if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
                continue;
            }
            BigDecimal remaining = InvoiceBalanceCalculator.remainingBalance(
                    invoice.getTotalAmount(), paidByInvoice.get(invoice.getId()));
            if (InvoiceBalanceCalculator.isOverdue(invoice.getDueDate(), remaining, today)) {
                count++;
            }
        }
        return count;
    }

    private List<Invoice> filterInvoicesByDate(List<Invoice> invoices, LocalDate fromDate,
            LocalDate toDate) {
        if (invoices.isEmpty() || (fromDate == null && toDate == null)) {
            return invoices;
        }
        return invoices.stream()
                .filter(inv -> {
                    LocalDate created = inv.getCreatedAt() != null
                            ? inv.getCreatedAt().toLocalDate()
                            : null;
                    if (created == null) return true;
                    if (fromDate != null && created.isBefore(fromDate)) return false;
                    if (toDate != null && created.isAfter(toDate)) return false;
                    return true;
                })
                .toList();
    }

    private Map<InvoiceStatus, Long> invoiceStatusCounts(List<Invoice> invoices,
            InvoiceStatus paymentStatus) {
        Map<InvoiceStatus, Long> counts = new EnumMap<>(InvoiceStatus.class);
        for (InvoiceStatus status : InvoiceStatus.values()) {
            counts.put(status, 0L);
        }
        for (Invoice invoice : invoices) {
            InvoiceStatus status = invoice.getStatus();
            if (paymentStatus != null && paymentStatus != status) {
                continue;
            }
            counts.merge(status, 1L, Long::sum);
        }
        return counts;
    }

    private BigDecimal sumRevenue(List<Invoice> invoices, LocalDate fromDate, LocalDate toDate) {
        List<Long> invoiceIds = invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                .map(Invoice::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (invoiceIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return paymentRepository.findByInvoiceIdIn(invoiceIds).stream()
                .filter(p -> isPaidRevenue(p, fromDate, toDate))
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isPaidRevenue(Payment payment, LocalDate fromDate, LocalDate toDate) {
        if (payment.getStatus() != PaymentStatus.PAID) {
            return false;
        }
        var paidAt = payment.getPaidAt() != null ? payment.getPaidAt().toLocalDate()
                : payment.getCreatedAt() != null ? payment.getCreatedAt().toLocalDate() : null;
        if (paidAt == null) {
            return true;
        }
        if (fromDate != null && paidAt.isBefore(fromDate)) return false;
        if (toDate != null && paidAt.isAfter(toDate)) return false;
        return true;
    }

    private List<ManagerDashboardResponse.RevenuePoint> buildRevenueTrend(List<Invoice> invoices,
            LocalDate fromDate, LocalDate toDate) {
        Map<String, BigDecimal> byMonth = new java.util.TreeMap<>();
        List<Long> invoiceIds = invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                .map(Invoice::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (!invoiceIds.isEmpty()) {
            for (Payment payment : paymentRepository.findByInvoiceIdIn(invoiceIds)) {
                if (!isPaidRevenue(payment, fromDate, toDate)) {
                    continue;
                }
                LocalDate date = payment.getPaidAt() != null ? payment.getPaidAt().toLocalDate()
                        : payment.getCreatedAt() != null ? payment.getCreatedAt().toLocalDate() : null;
                if (date == null) continue;
                String key = date.format(MONTH_FORMAT);
                byMonth.merge(key, payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO,
                        BigDecimal::add);
            }
        }

        // Fill gaps so the line chart is continuous over the requested range.
        YearMonth start = fromDate != null ? YearMonth.from(fromDate) : firstMonthOf(byMonth);
        YearMonth end = toDate != null ? YearMonth.from(toDate) : lastMonthOf(byMonth);
        if (start == null || end == null) {
            return List.of();
        }
        List<ManagerDashboardResponse.RevenuePoint> points = new ArrayList<>();
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            String key = ym.format(MONTH_FORMAT);
            points.add(new ManagerDashboardResponse.RevenuePoint(key,
                    byMonth.getOrDefault(key, BigDecimal.ZERO)));
        }
        return points;
    }

    private YearMonth firstMonthOf(Map<String, BigDecimal> byMonth) {
        return byMonth.isEmpty() ? null : YearMonth.parse(byMonth.keySet().stream().findFirst().get());
    }

    private YearMonth lastMonthOf(Map<String, BigDecimal> byMonth) {
        if (byMonth.isEmpty()) return null;
        String last = new ArrayList<>(byMonth.keySet()).get(byMonth.size() - 1);
        return YearMonth.parse(last);
    }

    private List<ManagerDashboardResponse.RoomOccupancyPoint> buildRoomOccupancy(
            List<Long> scopeHouseIds, RoomStatus roomStatus, Map<RoomStatus, Long> roomStatusCounts) {
        List<RoomStatus> statuses = roomStatus != null
                ? List.of(roomStatus)
                : List.of(RoomStatus.OCCUPIED, RoomStatus.AVAILABLE, RoomStatus.MAINTENANCE);

        if (scopeHouseIds.size() == 1) {
            long occupied = statusCount(roomStatusCounts, statuses, RoomStatus.OCCUPIED);
            long available = statusCount(roomStatusCounts, statuses, RoomStatus.AVAILABLE);
            long maintenance = statusCount(roomStatusCounts, statuses, RoomStatus.MAINTENANCE);
            return List.of(new ManagerDashboardResponse.RoomOccupancyPoint(
                    "បន្ទប់ទាំងអស់", occupied, available, maintenance));
        }

        List<Long> houseIds = roomRepository.findByHouseIdIn(scopeHouseIds).stream()
                .map(Room::getHouseId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        return houseIds.stream()
                .map(houseId -> {
                    long occupied = statusCountForHouse(houseId, statuses, RoomStatus.OCCUPIED);
                    long available = statusCountForHouse(houseId, statuses, RoomStatus.AVAILABLE);
                    long maintenance = statusCountForHouse(houseId, statuses, RoomStatus.MAINTENANCE);
                    return new ManagerDashboardResponse.RoomOccupancyPoint(
                            "House #" + houseId, occupied, available, maintenance);
                })
                .toList();
    }

    private long statusCount(Map<RoomStatus, Long> counts, List<RoomStatus> statuses,
            RoomStatus status) {
        if (!statuses.contains(status)) return 0L;
        return counts.getOrDefault(status, 0L);
    }

    private long statusCountForHouse(Long houseId, List<RoomStatus> statuses, RoomStatus status) {
        if (!statuses.contains(status)) return 0L;
        return roomRepository.countByHouseIdInAndStatus(List.of(houseId), status);
    }

    private List<ManagerDashboardResponse.StatusCount> buildMaintenanceStatus(
            List<Long> scopeHouseIds, LocalDate fromDate, LocalDate toDate) {
        List<MaintenanceRequest> requests =
                maintenanceRequestRepository.findAllByHouseIdInOrderByCreatedAtDesc(scopeHouseIds);
        if (fromDate != null || toDate != null) {
            requests = requests.stream()
                    .filter(r -> {
                        LocalDate reported = r.getReportedAt() != null
                                ? r.getReportedAt().toLocalDate() : null;
                        if (reported == null) return true;
                        if (fromDate != null && reported.isBefore(fromDate)) return false;
                        if (toDate != null && reported.isAfter(toDate)) return false;
                        return true;
                    })
                    .toList();
        }
        if (requests.isEmpty()) {
            return List.of(
                    new ManagerDashboardResponse.StatusCount("PENDING", 0),
                    new ManagerDashboardResponse.StatusCount("IN_PROGRESS", 0),
                    new ManagerDashboardResponse.StatusCount("RESOLVED", 0),
                    new ManagerDashboardResponse.StatusCount("CANCELLED", 0));
        }
        Map<String, Long> counts = requests.stream()
                .map(r -> r.getStatus() != null ? r.getStatus().name() : "UNKNOWN")
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        for (String status : List.of("PENDING", "IN_PROGRESS", "RESOLVED", "CANCELLED")) {
            counts.putIfAbsent(status, 0L);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ManagerDashboardResponse.StatusCount(e.getKey(), e.getValue()))
                .toList();
    }

    private static final List<RoomStatus> KNOWN_ROOM_STATUSES =
            List.of(RoomStatus.AVAILABLE, RoomStatus.BOOKED, RoomStatus.OCCUPIED,
                    RoomStatus.MAINTENANCE, RoomStatus.RESERVED);
}
