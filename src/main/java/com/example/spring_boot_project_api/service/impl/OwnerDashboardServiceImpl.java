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
import com.example.spring_boot_project_api.dto.response.OwnerDashboardResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.MaintenanceRequest;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.repository.ContractDepositRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.MaintenanceRequestRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.util.InvoiceBalanceCalculator;
import com.example.spring_boot_project_api.service.OwnerDashboardService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OwnerDashboardServiceImpl implements OwnerDashboardService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final RentalContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final TenantRepository tenantRepository;
    private final ContractDepositRepository depositRepository;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional(readOnly = true)
    public OwnerDashboardResponse getDashboard(Long houseId, LocalDate fromDate, LocalDate toDate) {
        List<Long> scopeHouseIds = resolveScopeHouseIds(houseId);

        if (scopeHouseIds.isEmpty()) {
            return emptyDashboard();
        }

        long totalHouses = scopeHouseIds.size();
        long totalRooms = roomRepository.countByHouseIdIn(scopeHouseIds);
        long occupiedRooms = roomRepository.countByHouseIdInAndStatus(scopeHouseIds, RoomStatus.OCCUPIED);
        long totalActiveTenants = tenantRepository.countActiveTenantsByHouseIds(
                scopeHouseIds, LocalDate.now());
        double occupancyRate = totalRooms == 0 ? 0.0
                : BigDecimal.valueOf(occupiedRooms)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalRooms), 1, RoundingMode.HALF_UP)
                        .doubleValue();

        List<Long> contractIds = contractRepository.findByRoomHouseIdIn(scopeHouseIds).stream()
                .map(RentalContract::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Invoice> scopeInvoices = contractIds.isEmpty()
                ? List.of()
                : invoiceRepository.findByContractIdIn(contractIds);

        BigDecimal totalRevenue = sumRevenue(scopeInvoices, fromDate, toDate);

        // Overdue is derived from due date + outstanding balance across the whole scope,
        // not restricted by the selected dashboard date range. PAID totals are aggregated
        // once for every scoped invoice so no per-invoice payment query is issued.
        Map<Long, BigDecimal> paidByInvoice = paidAmountsByInvoiceId(scopeInvoices);
        BigDecimal overdueAmount = overdueBalance(scopeInvoices, paidByInvoice, LocalDate.now());

        List<MaintenanceRequest> allMaintenance =
                maintenanceRequestRepository.findAllByHouseIdInOrderByCreatedAtDesc(scopeHouseIds);

        BigDecimal paidDeposit = depositRepository.sumPaidAmountByHouseIds(scopeHouseIds);
        if (paidDeposit == null) {
            paidDeposit = BigDecimal.ZERO;
        }

        List<ManagerDashboardResponse.RevenuePoint> revenueTrend =
                buildRevenueTrend(scopeInvoices, fromDate, toDate);

        List<ManagerDashboardResponse.RoomOccupancyPoint> roomOccupancy =
                buildRoomOccupancy(scopeHouseIds);

        Map<InvoiceStatus, Long> invoiceStatusCounts = new EnumMap<>(InvoiceStatus.class);
        for (InvoiceStatus status : InvoiceStatus.values()) {
            invoiceStatusCounts.put(status, 0L);
        }
        for (Invoice inv : scopeInvoices) {
                        if (inv.getStatus() != null) {
                                invoiceStatusCounts.merge(inv.getStatus(), 1L, Long::sum);
                        }
        }
        List<ManagerDashboardResponse.StatusCount> paymentStatusBreakdown =
                invoiceStatusCounts.entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getKey().name()))
                        .map(e -> new ManagerDashboardResponse.StatusCount(e.getKey().name(), e.getValue()))
                        .toList();

        List<ManagerDashboardResponse.StatusCount> maintenanceStatus =
                buildMaintenanceStatus(allMaintenance);

        List<OwnerDashboardResponse.HouseRevenuePoint> revenueByHouse =
                buildRevenueByHouse(scopeHouseIds, fromDate, toDate);

        List<OwnerDashboardResponse.TopPerformingHouse> topHouses =
                buildTopHouses(scopeHouseIds, fromDate, toDate, paidByInvoice);

        List<OwnerDashboardResponse.RecentPayment> recentPayments =
                buildRecentPayments(scopeHouseIds);

        List<OwnerDashboardResponse.RecentActivity> recentActivities =
                buildRecentActivities(scopeHouseIds, allMaintenance);

        return new OwnerDashboardResponse(
                new OwnerDashboardResponse.OwnerKpis(
                        totalHouses, totalRooms, totalActiveTenants,
                        occupancyRate, totalRevenue, overdueAmount, paidDeposit),
                revenueTrend,
                roomOccupancy,
                paymentStatusBreakdown,
                maintenanceStatus,
                revenueByHouse,
                topHouses,
                recentPayments,
                recentActivities);
    }

    private List<Long> resolveScopeHouseIds(Long houseId) {
        List<Long> accessible = houseAccessService.getAccessibleHouseIds();
        if (houseId == null) {
            return accessible;
        }
        houseAccessService.requireHouseAccess(houseId);
        return List.of(houseId);
    }

    private OwnerDashboardResponse emptyDashboard() {
        return new OwnerDashboardResponse(
                new OwnerDashboardResponse.OwnerKpis(
                        0, 0, 0, 0.0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /**
     * PAID payment totals per invoice, aggregated in a single grouped query. Reusing this
     * map keeps the dashboard free of per-invoice payment lookups.
     */
    private Map<Long, BigDecimal> paidAmountsByInvoiceId(List<Invoice> invoices) {
        List<Long> invoiceIds = invoices.stream()
                .map(Invoice::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, BigDecimal> paidByInvoice = new HashMap<>();
        if (invoiceIds.isEmpty()) {
            return paidByInvoice;
        }
        List<Object[]> rows = paymentRepository.sumPaidAmountByInvoiceIds(invoiceIds, PaymentStatus.PAID);
        if (rows == null) {
            return paidByInvoice;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            BigDecimal amount = row[1] instanceof BigDecimal value
                    ? value : new BigDecimal(row[1].toString());
            paidByInvoice.put(((Number) row[0]).longValue(), amount);
        }
        return paidByInvoice;
    }

    /**
     * Overdue exposure for the given invoices: the outstanding balance of every invoice
     * whose due date has already passed.
     *
     * <p>The persisted {@code totalAmount} is already {@code subtotal - discount + lateFee}
     * (see {@code InvoiceBalanceCalculator.invoiceTotal}), so it is used directly instead of
     * re-deriving it from the lazy item collection, and the discount is never subtracted twice.
     * Only PAID payments reduce the balance; a fully paid invoice contributes nothing.
     */
    private BigDecimal overdueBalance(List<Invoice> invoices, Map<Long, BigDecimal> paidByInvoice,
            LocalDate asOf) {
        BigDecimal total = BigDecimal.ZERO;
        for (Invoice invoice : invoices) {
            if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
                continue;
            }
            BigDecimal remaining = InvoiceBalanceCalculator.remainingBalance(
                    invoice.getTotalAmount(), paidByInvoice.get(invoice.getId()));
            if (InvoiceBalanceCalculator.isOverdue(invoice.getDueDate(), remaining, asOf)) {
                total = total.add(remaining);
            }
        }
        return total;
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

    private List<ManagerDashboardResponse.RevenuePoint> buildRevenueTrend(
            List<Invoice> invoices, LocalDate fromDate, LocalDate toDate) {
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
                byMonth.merge(key,
                        payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO,
                        BigDecimal::add);
            }
        }

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
        return byMonth.isEmpty() ? null
                : YearMonth.parse(byMonth.keySet().stream().findFirst().get());
    }

    private YearMonth lastMonthOf(Map<String, BigDecimal> byMonth) {
        if (byMonth.isEmpty()) return null;
        String last = new ArrayList<>(byMonth.keySet()).get(byMonth.size() - 1);
        return YearMonth.parse(last);
    }

    private List<ManagerDashboardResponse.RoomOccupancyPoint> buildRoomOccupancy(
            List<Long> scopeHouseIds) {
        List<Long> distinctHouseIds = roomRepository.findByHouseIdIn(scopeHouseIds).stream()
                .map(Room::getHouseId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        Map<Long, String> houseNames = houseRepository.findAllById(distinctHouseIds).stream()
                .collect(Collectors.toMap(House::getId, House::getName, (a, b) -> a));

        return distinctHouseIds.stream()
                .map(hId -> {
                    long occupied = roomRepository.countByHouseIdInAndStatus(
                            List.of(hId), RoomStatus.OCCUPIED);
                    long available = roomRepository.countByHouseIdInAndStatus(
                            List.of(hId), RoomStatus.AVAILABLE);
                    long maintenance = roomRepository.countByHouseIdInAndStatus(
                            List.of(hId), RoomStatus.MAINTENANCE);
                    return new ManagerDashboardResponse.RoomOccupancyPoint(
                            houseNames.getOrDefault(hId, "House #" + hId),
                            occupied, available, maintenance);
                })
                .toList();
    }

    private List<ManagerDashboardResponse.StatusCount> buildMaintenanceStatus(
            List<MaintenanceRequest> requests) {
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

    private List<OwnerDashboardResponse.HouseRevenuePoint> buildRevenueByHouse(
            List<Long> scopeHouseIds, LocalDate fromDate, LocalDate toDate) {
        Map<Long, String> houseNames = houseRepository.findAllById(scopeHouseIds).stream()
                .collect(Collectors.toMap(House::getId, House::getName, (a, b) -> a));

        return scopeHouseIds.stream()
                .map(hId -> {
                    List<Long> hContractIds = contractRepository.findByRoomHouseIdIn(List.of(hId))
                            .stream()
                            .map(RentalContract::getId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();
                    List<Invoice> hInvoices = hContractIds.isEmpty()
                            ? List.of()
                            : invoiceRepository.findByContractIdIn(hContractIds);
                    BigDecimal revenue = sumRevenue(hInvoices, fromDate, toDate);
                    return new OwnerDashboardResponse.HouseRevenuePoint(
                            houseNames.getOrDefault(hId, "House #" + hId), revenue);
                })
                .filter(p -> p.revenue().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(
                        OwnerDashboardResponse.HouseRevenuePoint::revenue,
                        Comparator.reverseOrder()))
                .toList();
    }

    private List<OwnerDashboardResponse.TopPerformingHouse> buildTopHouses(
            List<Long> scopeHouseIds, LocalDate fromDate, LocalDate toDate,
            Map<Long, BigDecimal> paidByInvoice) {
        Map<Long, String> houseNames = houseRepository.findAllById(scopeHouseIds).stream()
                .collect(Collectors.toMap(House::getId, House::getName, (a, b) -> a));

        return scopeHouseIds.stream()
                .map(hId -> {
                    long totalRooms = roomRepository.countByHouseIdIn(List.of(hId));
                    long occupied = roomRepository.countByHouseIdInAndStatus(
                            List.of(hId), RoomStatus.OCCUPIED);
                    double occupancy = totalRooms == 0 ? 0.0
                            : BigDecimal.valueOf(occupied)
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(totalRooms), 1, RoundingMode.HALF_UP)
                                    .doubleValue();

                    List<Long> hContractIds = contractRepository.findByRoomHouseIdIn(List.of(hId))
                            .stream()
                            .map(RentalContract::getId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();
                    List<Invoice> hInvoices = hContractIds.isEmpty()
                            ? List.of()
                            : invoiceRepository.findByContractIdIn(hContractIds);
                    BigDecimal revenue = sumRevenue(hInvoices, fromDate, toDate);
                    BigDecimal overdue = overdueBalance(hInvoices, paidByInvoice, LocalDate.now());

                    return new OwnerDashboardResponse.TopPerformingHouse(
                            houseNames.getOrDefault(hId, "House #" + hId),
                            totalRooms, occupancy, revenue, overdue);
                })
                .sorted(Comparator.comparing(
                        OwnerDashboardResponse.TopPerformingHouse::revenue,
                        Comparator.reverseOrder()))
                .toList();
    }

    private List<OwnerDashboardResponse.RecentPayment> buildRecentPayments(
            List<Long> scopeHouseIds) {
        List<Long> contractIds = contractRepository.findByRoomHouseIdIn(scopeHouseIds).stream()
                .map(RentalContract::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (contractIds.isEmpty()) {
            return List.of();
        }

        List<Room> rooms = roomRepository.findByHouseIdIn(scopeHouseIds);
        Map<Long, Long> roomToHouseId = rooms.stream()
                .filter(room -> room.getId() != null && room.getHouseId() != null)
                .collect(Collectors.toMap(Room::getId, Room::getHouseId, (a, b) -> a));
        Map<Long, String> roomNumbers = rooms.stream()
                .filter(room -> room.getId() != null)
                .collect(Collectors.toMap(Room::getId,
                        room -> room.getRoomNumber() != null ? room.getRoomNumber() : "UNKNOWN",
                        (a, b) -> a));

        Map<Long, Long> contractToRoomId = contractRepository.findByRoomHouseIdIn(scopeHouseIds)
                .stream()
                .filter(contract -> contract.getId() != null && contract.getRoomId() != null)
                .collect(Collectors.toMap(
                        RentalContract::getId, RentalContract::getRoomId, (a, b) -> a));

        List<Invoice> invoices = invoiceRepository.findByContractIdIn(contractIds);
        Map<Long, Invoice> invoiceMap = invoices.stream()
                .filter(invoice -> invoice.getId() != null)
                .collect(Collectors.toMap(Invoice::getId, i -> i, (a, b) -> a));

        List<Long> invoiceIds = new ArrayList<>(invoiceMap.keySet());
        if (invoiceIds.isEmpty()) {
            return List.of();
        }

        Map<Long, String> houseNames = houseRepository.findAllById(scopeHouseIds).stream()
                .filter(house -> house.getId() != null)
                .collect(Collectors.toMap(House::getId,
                        house -> house.getName() != null ? house.getName() : "UNKNOWN",
                        (a, b) -> a));

        return paymentRepository.findByInvoiceIdIn(invoiceIds).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .sorted(Comparator.comparing(
                        (Payment p) -> p.getPaidAt() != null ? p.getPaidAt() : p.getCreatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(p -> {
                    Invoice inv = invoiceMap.get(p.getInvoiceId());
                    Long cId = inv != null ? inv.getContractId() : null;
                    Long rId = cId != null ? contractToRoomId.get(cId) : null;
                    Long hId = rId != null ? roomToHouseId.get(rId) : null;
                    return new OwnerDashboardResponse.RecentPayment(
                            p.getId(),
                            p.getPaymentNo(),
                            p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO,
                            p.getStatus() != null ? p.getStatus().name() : "UNKNOWN",
                            p.getPaidAt(),
                            null,
                            hId != null ? houseNames.getOrDefault(hId, "House #" + hId) : null,
                            rId != null ? roomNumbers.get(rId) : null);
                })
                .toList();
    }

    private List<OwnerDashboardResponse.RecentActivity> buildRecentActivities(
            List<Long> scopeHouseIds, List<MaintenanceRequest> maintenanceRequests) {
        List<OwnerDashboardResponse.RecentActivity> activities = new ArrayList<>();

        maintenanceRequests.stream()
                .limit(5)
                .forEach(r -> activities.add(new OwnerDashboardResponse.RecentActivity(
                        "MAINTENANCE",
                        r.getTitle(),
                        r.getHouse() != null ? r.getHouse().getName() : null,
                        r.getCreatedAt())));

        List<Long> contractIds = contractRepository.findByRoomHouseIdIn(scopeHouseIds).stream()
                .map(RentalContract::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!contractIds.isEmpty()) {
            List<Invoice> invoices = invoiceRepository.findByContractIdIn(contractIds);
            List<Long> invoiceIds = invoices.stream()
                    .map(Invoice::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!invoiceIds.isEmpty()) {
                paymentRepository.findByInvoiceIdIn(invoiceIds).stream()
                        .filter(p -> p.getStatus() == PaymentStatus.PAID)
                        .sorted(Comparator.comparing(
                                (Payment p) -> p.getPaidAt() != null
                                        ? p.getPaidAt() : p.getCreatedAt(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(5)
                        .forEach(p -> activities.add(new OwnerDashboardResponse.RecentActivity(
                                "PAYMENT",
                                "Payment received: $" + p.getAmount(),
                                null,
                                p.getPaidAt() != null ? p.getPaidAt() : p.getCreatedAt())));
            }
        }

        return activities.stream()
                .sorted(Comparator.comparing(
                        OwnerDashboardResponse.RecentActivity::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .toList();
    }
}
