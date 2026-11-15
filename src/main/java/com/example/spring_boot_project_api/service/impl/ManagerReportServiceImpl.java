package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.ManagerMonthlyReportResponse;
import com.example.spring_boot_project_api.enums.InvoiceItemType;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.InvalidReportQueryException;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.MaintenanceRequestStatus;
import com.example.spring_boot_project_api.repository.ContractDepositRepository;
import com.example.spring_boot_project_api.repository.ContractRenewalRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.MaintenanceCostRepository;
import com.example.spring_boot_project_api.repository.MaintenanceRequestRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UtilityReadingRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.ManagerReportService;
import com.example.spring_boot_project_api.util.InvoiceBalanceCalculator;

import lombok.RequiredArgsConstructor;

/**
 * Monthly report built entirely from database aggregations (COUNT/SUM/GROUP BY).
 * The period is treated as [first day of the month, first day of the next month),
 * i.e. date >= startOfMonth AND date < startOfNextMonth — no 23:59:59 hacks.
 */
@Service
@RequiredArgsConstructor
public class ManagerReportServiceImpl implements ManagerReportService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final List<InvoiceStatus> OUTSTANDING_INVOICE_STATUSES =
            List.of(InvoiceStatus.PENDING, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE);
    private static final List<InvoiceItemType> UTILITY_ITEM_TYPES =
            List.of(InvoiceItemType.WATER, InvoiceItemType.ELECTRICITY);

    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final RentalContractRepository contractRepository;
    private final ContractRenewalRepository contractRenewalRepository;
    private final ContractDepositRepository contractDepositRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final MaintenanceCostRepository maintenanceCostRepository;
    private final UtilityReadingRepository utilityReadingRepository;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional(readOnly = true)
    public ManagerMonthlyReportResponse getMonthlyReport(int year, int month, Long houseId) {
        validatePeriod(year, month);
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime periodStart = ym.atDay(1).atStartOfDay();
        LocalDateTime periodEnd = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<Long> scopeHouseIds = resolveScopeHouseIds(houseId);
        String houseName = resolveHouseName(houseId);

        if (scopeHouseIds.isEmpty()) {
            return emptyReport(ym, periodStart, periodEnd, houseId, houseName);
        }

        ManagerMonthlyReportResponse.RoomStats rooms = roomStats(scopeHouseIds);
        ManagerMonthlyReportResponse.FinancialSummary financial =
                financialSummary(ym, periodStart, periodEnd, scopeHouseIds);
        ManagerMonthlyReportResponse.ContractSummary contracts =
                contractSummary(ym, periodStart, periodEnd, scopeHouseIds);
        ManagerMonthlyReportResponse.PaymentSummary payments =
                paymentSummary(ym, periodStart, periodEnd, scopeHouseIds);
        ManagerMonthlyReportResponse.UtilitySummary utilities =
                utilitySummary(ym, periodStart, periodEnd, scopeHouseIds);
        ManagerMonthlyReportResponse.MaintenanceSummary maintenance =
                maintenanceSummary(periodStart, periodEnd, scopeHouseIds);

        List<ManagerMonthlyReportResponse.MonthlyRevenuePoint> revenueTrend =
                revenueTrend(ym, periodStart, periodEnd, scopeHouseIds);
        List<ManagerMonthlyReportResponse.StatusCount> paymentStatus =
                invoiceStatusCounts(ym, scopeHouseIds);
        List<ManagerMonthlyReportResponse.StatusCount> roomOccupancy =
                roomOccupancy(scopeHouseIds);
        List<ManagerMonthlyReportResponse.StatusCount> maintenanceStatus =
                maintenanceStatusCounts(periodStart, periodEnd, scopeHouseIds);

        return new ManagerMonthlyReportResponse(
                new ManagerMonthlyReportResponse.ReportMeta(
                        year, month, periodStart, periodEnd, houseId, houseName, LocalDateTime.now()),
                rooms, financial, contracts, payments, utilities, maintenance,
                revenueTrend, paymentStatus, roomOccupancy, maintenanceStatus);
    }

    private void validatePeriod(int year, int month) {
        if (month < 1 || month > 12) {
            throw new InvalidReportQueryException("Month must be between 1 and 12");
        }
        if (year < 2000 || year > 2100) {
            throw new InvalidReportQueryException("Year must be between 2000 and 2100");
        }
    }

    private List<Long> resolveScopeHouseIds(Long houseId) {
        if (houseId == null) {
            return houseAccessService.getAccessibleHouseIds();
        }
        // Throws AccessDeniedException when the user is not authorized for the house.
        houseAccessService.requireHouseAccess(houseId);
        return List.of(houseId);
    }

    private String resolveHouseName(Long houseId) {
        if (houseId == null) {
            return null;
        }
        return houseRepository.findById(houseId).map(House::getName).orElse(null);
    }

    private ManagerMonthlyReportResponse emptyReport(YearMonth ym, LocalDateTime periodStart,
            LocalDateTime periodEnd, Long houseId, String houseName) {
        return new ManagerMonthlyReportResponse(
                new ManagerMonthlyReportResponse.ReportMeta(
                        ym.getYear(), ym.getMonthValue(), periodStart, periodEnd,
                        houseId, houseName, LocalDateTime.now()),
                new ManagerMonthlyReportResponse.RoomStats(0, 0, 0, 0, 0, 0.0),
                new ManagerMonthlyReportResponse.FinancialSummary(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO),
                new ManagerMonthlyReportResponse.ContractSummary(0, 0, 0, 0, 0),
                new ManagerMonthlyReportResponse.PaymentSummary(0, 0, 0, BigDecimal.ZERO),
                new ManagerMonthlyReportResponse.UtilitySummary(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0),
                new ManagerMonthlyReportResponse.MaintenanceSummary(0, 0, 0, 0, 0, BigDecimal.ZERO),
                List.of(),
                zeroedInvoiceStatuses(),
                zeroedRoomStatuses(),
                zeroedMaintenanceStatuses());
    }

    private ManagerMonthlyReportResponse.RoomStats roomStats(List<Long> scopeHouseIds) {
        long total = 0;
        long occupied = 0;
        long available = 0;
        long reserved = 0;
        long maintenance = 0;
        for (RoomStatus status : RoomStatus.values()) {
            long count = roomRepository.countByHouseIdInAndStatus(scopeHouseIds, status);
            total += count;
            switch (status) {
                case OCCUPIED -> occupied = count;
                case AVAILABLE -> available = count;
                case RESERVED -> reserved = count;
                case MAINTENANCE -> maintenance = count;
            }
        }
        double occupancyRate = total == 0 ? 0.0
                : BigDecimal.valueOf(occupied)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                        .doubleValue();
        return new ManagerMonthlyReportResponse.RoomStats(
                total, occupied, available, reserved, maintenance, occupancyRate);
    }

    private ManagerMonthlyReportResponse.FinancialSummary financialSummary(YearMonth ym,
            LocalDateTime periodStart, LocalDateTime periodEnd, List<Long> scopeHouseIds) {
        BigDecimal totalRent = invoiceRepository.sumRentAmountByYearMonthHouseIds(
                ym.getYear(), ym.getMonthValue(), scopeHouseIds);
        BigDecimal totalPaid = paymentRepository.sumPaidAmountByHouseIdsAndPaidAtRange(
                periodStart, periodEnd, scopeHouseIds);
        BigDecimal totalOutstanding = invoiceRepository.sumOutstandingAmountByYearMonthHouseIds(
                ym.getYear(), ym.getMonthValue(), scopeHouseIds, OUTSTANDING_INVOICE_STATUSES);
        BigDecimal totalDeposits = contractDepositRepository.sumPaidAmountByHouseIdsAndPaidAtRange(
                periodStart, periodEnd, scopeHouseIds);
        BigDecimal totalExpenses = maintenanceCostRepository.sumTotalAmountByHouseIdsAndCreatedAtRange(
                periodStart, periodEnd, scopeHouseIds);
        BigDecimal netRevenue = totalPaid.add(totalDeposits).subtract(totalExpenses);
        return new ManagerMonthlyReportResponse.FinancialSummary(
                totalRent, totalPaid, totalOutstanding, totalDeposits, totalExpenses, netRevenue);
    }

    private ManagerMonthlyReportResponse.ContractSummary contractSummary(YearMonth ym,
            LocalDateTime periodStart, LocalDateTime periodEnd, List<Long> scopeHouseIds) {
        LocalDate monthStart = ym.atDay(1);
        LocalDate nextMonth = ym.plusMonths(1).atDay(1);
        return new ManagerMonthlyReportResponse.ContractSummary(
                contractRepository.countCreatedByHouseIdsAndRange(periodStart, periodEnd, scopeHouseIds),
                contractRepository.countActiveOverlappingMonthByHouseIds(monthStart, nextMonth, scopeHouseIds),
                contractRepository.countExpiredWithinMonthByHouseIds(monthStart, nextMonth, scopeHouseIds),
                contractRepository.countTerminatedWithinMonthByHouseIds(monthStart, nextMonth, scopeHouseIds),
                contractRenewalRepository.countByHouseIdsAndRenewedAtRange(periodStart, periodEnd, scopeHouseIds));
    }

    private ManagerMonthlyReportResponse.PaymentSummary paymentSummary(YearMonth ym,
            LocalDateTime periodStart, LocalDateTime periodEnd, List<Long> scopeHouseIds) {
        long paidPayments = paymentRepository.countByHouseIdsAndStatusAndPaidAtRange(
                periodStart, periodEnd, scopeHouseIds, PaymentStatus.PAID);
        long pendingPayments = paymentRepository.countByHouseIdsAndStatusAndCreatedAtRange(
                periodStart, periodEnd, scopeHouseIds, PaymentStatus.PENDING);
        // "Overdue payments" counts the invoices billed for the month that are past their due
        // date and still owe money. It stays a count, and it is derived rather than read from
        // InvoiceStatus.OVERDUE, which the invoice lifecycle never persists.
        long overduePayments = countOverdueInvoices(ym, scopeHouseIds);
        BigDecimal totalPaymentAmount = paymentRepository.sumPaidAmountByHouseIdsAndPaidAtRange(
                periodStart, periodEnd, scopeHouseIds);
        return new ManagerMonthlyReportResponse.PaymentSummary(
                paidPayments, pendingPayments, overduePayments, totalPaymentAmount);
    }

    /**
     * Counts the month's invoices that are past their due date and still owe money. PAID
     * payment totals come from a single grouped query, so no per-invoice lookup is added.
     */
    private long countOverdueInvoices(YearMonth ym, List<Long> scopeHouseIds) {
        if (scopeHouseIds == null || scopeHouseIds.isEmpty()) {
            return 0L;
        }
        List<Invoice> invoices = invoiceRepository.findByYearMonthHouseIds(
                ym.getYear(), ym.getMonthValue(), scopeHouseIds);
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

    private ManagerMonthlyReportResponse.UtilitySummary utilitySummary(YearMonth ym,
            LocalDateTime periodStart, LocalDateTime periodEnd, List<Long> scopeHouseIds) {
        List<Object[]> usageRows = utilityReadingRepository
                .sumUsageGroupByTypeCodeByHouseIdsAndDateRange(
                        periodStart.toLocalDate(), periodEnd.toLocalDate(), scopeHouseIds);
        BigDecimal waterUsage = BigDecimal.ZERO;
        BigDecimal electricityUsage = BigDecimal.ZERO;
        for (Object[] row : usageRows) {
            String code = String.valueOf(row[0]);
            BigDecimal amount = row[1] instanceof BigDecimal bd ? bd
                    : new BigDecimal(String.valueOf(row[1]));
            if (isWaterCode(code)) {
                waterUsage = waterUsage.add(amount);
            } else if (isElectricityCode(code)) {
                electricityUsage = electricityUsage.add(amount);
            }
        }
        BigDecimal totalUtilityCharges = invoiceRepository.sumItemAmountsByYearMonthHouseIds(
                ym.getYear(), ym.getMonthValue(), scopeHouseIds, UTILITY_ITEM_TYPES);
        long paidUtilityBills = invoiceRepository.countUtilityBillsByYearMonthHouseIdsAndStatusIn(
                ym.getYear(), ym.getMonthValue(), scopeHouseIds,
                List.of(InvoiceStatus.PAID), UTILITY_ITEM_TYPES);
        long outstandingUtilityBills = invoiceRepository.countUtilityBillsByYearMonthHouseIdsAndStatusIn(
                ym.getYear(), ym.getMonthValue(), scopeHouseIds,
                OUTSTANDING_INVOICE_STATUSES, UTILITY_ITEM_TYPES);
        return new ManagerMonthlyReportResponse.UtilitySummary(
                waterUsage, electricityUsage, totalUtilityCharges,
                paidUtilityBills, outstandingUtilityBills);
    }

    private ManagerMonthlyReportResponse.MaintenanceSummary maintenanceSummary(
            LocalDateTime periodStart, LocalDateTime periodEnd, List<Long> scopeHouseIds) {
        Map<MaintenanceRequestStatus, Long> counts = new EnumMap<>(MaintenanceRequestStatus.class);
        for (MaintenanceRequestStatus status : MaintenanceRequestStatus.values()) {
            counts.put(status, 0L);
        }
        List<Object[]> rows = maintenanceRequestRepository
                .countGroupByStatusAndHouseIdsAndReportedAtRange(periodStart, periodEnd, scopeHouseIds);
        long total = 0;
        for (Object[] row : rows) {
            MaintenanceRequestStatus status =
                    MaintenanceRequestStatus.valueOf(String.valueOf(row[0]));
            long count = ((Number) row[1]).longValue();
            counts.put(status, count);
            total += count;
        }
        return new ManagerMonthlyReportResponse.MaintenanceSummary(
                total,
                counts.get(MaintenanceRequestStatus.PENDING),
                counts.get(MaintenanceRequestStatus.IN_PROGRESS),
                counts.get(MaintenanceRequestStatus.RESOLVED),
                counts.get(MaintenanceRequestStatus.CANCELLED),
                maintenanceCostRepository.sumTotalAmountByHouseIdsAndCreatedAtRange(
                        periodStart, periodEnd, scopeHouseIds));
    }

    private List<ManagerMonthlyReportResponse.MonthlyRevenuePoint> revenueTrend(YearMonth ym,
            LocalDateTime periodStart, LocalDateTime periodEnd, List<Long> scopeHouseIds) {
        YearMonth firstMonth = ym.minusMonths(11);
        LocalDateTime trendStart = firstMonth.atDay(1).atStartOfDay();
        List<Object[]> rows = paymentRepository.sumPaidGroupByYearMonthByHouseIds(
                trendStart, periodEnd, scopeHouseIds);

        Map<YearMonth, BigDecimal> byMonth = new HashMap<>();
        for (Object[] row : rows) {
            int y = ((Number) row[0]).intValue();
            int m = ((Number) row[1]).intValue();
            BigDecimal amount = row[2] instanceof BigDecimal bd ? bd
                    : new BigDecimal(String.valueOf(row[2]));
            byMonth.put(YearMonth.of(y, m), amount);
        }

        List<ManagerMonthlyReportResponse.MonthlyRevenuePoint> points = new ArrayList<>();
        for (YearMonth cursor = firstMonth; !cursor.isAfter(ym); cursor = cursor.plusMonths(1)) {
            points.add(new ManagerMonthlyReportResponse.MonthlyRevenuePoint(
                    cursor.format(MONTH_FORMAT),
                    byMonth.getOrDefault(cursor, BigDecimal.ZERO)));
        }
        return points;
    }

    private List<ManagerMonthlyReportResponse.StatusCount> invoiceStatusCounts(
            YearMonth ym, List<Long> scopeHouseIds) {
        Map<String, Long> counts = new HashMap<>();
        for (InvoiceStatus status : InvoiceStatus.values()) {
            counts.put(status.name(), 0L);
        }
        List<Object[]> rows = invoiceRepository.countGroupByStatusByYearMonthHouseIds(
                ym.getYear(), ym.getMonthValue(), scopeHouseIds);
        for (Object[] row : rows) {
            counts.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ManagerMonthlyReportResponse.StatusCount(e.getKey(), e.getValue()))
                .toList();
    }

    private List<ManagerMonthlyReportResponse.StatusCount> roomOccupancy(List<Long> scopeHouseIds) {
        Map<RoomStatus, Long> counts = new EnumMap<>(RoomStatus.class);
        for (RoomStatus status : RoomStatus.values()) {
            counts.put(status, roomRepository.countByHouseIdInAndStatus(scopeHouseIds, status));
        }
        return counts.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .map(e -> new ManagerMonthlyReportResponse.StatusCount(e.getKey().name(), e.getValue()))
                .toList();
    }

    private List<ManagerMonthlyReportResponse.StatusCount> maintenanceStatusCounts(
            LocalDateTime periodStart, LocalDateTime periodEnd, List<Long> scopeHouseIds) {
        Map<String, Long> counts = new HashMap<>();
        counts.put(MaintenanceRequestStatus.PENDING.name(), 0L);
        counts.put(MaintenanceRequestStatus.IN_PROGRESS.name(), 0L);
        counts.put(MaintenanceRequestStatus.RESOLVED.name(), 0L);
        counts.put(MaintenanceRequestStatus.CANCELLED.name(), 0L);
        List<Object[]> rows = maintenanceRequestRepository
                .countGroupByStatusAndHouseIdsAndReportedAtRange(periodStart, periodEnd, scopeHouseIds);
        for (Object[] row : rows) {
            counts.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ManagerMonthlyReportResponse.StatusCount(e.getKey(), e.getValue()))
                .toList();
    }

    private List<ManagerMonthlyReportResponse.StatusCount> zeroedInvoiceStatuses() {
        return List.of(InvoiceStatus.values()).stream()
                .map(s -> new ManagerMonthlyReportResponse.StatusCount(s.name(), 0))
                .toList();
    }

    private List<ManagerMonthlyReportResponse.StatusCount> zeroedRoomStatuses() {
        return List.of(RoomStatus.values()).stream()
                .map(s -> new ManagerMonthlyReportResponse.StatusCount(s.name(), 0))
                .toList();
    }

    private List<ManagerMonthlyReportResponse.StatusCount> zeroedMaintenanceStatuses() {
        return List.of(MaintenanceRequestStatus.values()).stream()
                .map(s -> new ManagerMonthlyReportResponse.StatusCount(s.name(), 0))
                .toList();
    }

    private static boolean isWaterCode(String code) {
        String normalized = code == null ? "" : code.toUpperCase();
        return normalized.equals("WTR") || normalized.contains("WATER");
    }

    private static boolean isElectricityCode(String code) {
        String normalized = code == null ? "" : code.toUpperCase();
        return normalized.equals("ELEC") || normalized.contains("ELECTRIC");
    }
}