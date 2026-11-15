package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse.TenantDashboardContract;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse.TenantDashboardFinancial;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse.TenantDashboardRoom;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse.TenantDashboardTenant;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse.TenantDashboardUtilities;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceDetailResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceResponse;
import com.example.spring_boot_project_api.dto.response.TenantPaymentHistoryResponse;
import com.example.spring_boot_project_api.dto.response.TenantUtilityTrendResponse;
import com.example.spring_boot_project_api.dto.response.UtilityUsageResponse;
import com.example.spring_boot_project_api.enums.InvoiceItemType;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.exception.InvalidTenantQueryException;
import com.example.spring_boot_project_api.exception.InvoiceNotFoundException;
import com.example.spring_boot_project_api.mapper.TenantDashboardMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityMeter;
import com.example.spring_boot_project_api.model.UtilityReading;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityReadingRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.TenantDashboardService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantDashboardServiceImpl implements TenantDashboardService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_RANGE_MONTHS = 6;
    private static final List<InvoiceStatus> UNPAID_STATUSES = List.of(
            InvoiceStatus.PENDING, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE);

    private final TenantContextResolver tenantContextResolver;
    private final SecurityUtils securityUtils;
    private final ContractTenantRepository contractTenantRepository;
    private final RentalContractRepository rentalContractRepository;
    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final UtilityMeterRepository utilityMeterRepository;
    private final UtilityReadingRepository utilityReadingRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TenantDashboardMapper tenantDashboardMapper;

    @Override
    @Transactional(readOnly = true)
    public TenantDashboardResponse getDashboard() {
        Tenant tenant = requireCurrentTenant();

        RentalContract contract = tenantContextResolver.activeContractOf(tenant.getId());
        Room room = null;
        House house = null;
        if (contract != null) {
            room = roomRepository.findById(contract.getRoomId()).orElse(null);
            house = room != null ? houseRepository.findById(room.getHouseId()).orElse(null) : null;
        }

        List<Long> contractIds = tenantContractIds(tenant.getId());
        List<Invoice> unpaid;
        long paidCount;
        BigDecimal totalPaid = BigDecimal.ZERO;
        if (contractIds.isEmpty()) {
            unpaid = List.of();
            paidCount = 0;
        } else {
            unpaid = invoiceRepository.findByContractIdInAndStatusIn(contractIds, UNPAID_STATUSES);
            paidCount = invoiceRepository.countByContractIdInAndStatus(contractIds, InvoiceStatus.PAID);
            BigDecimal total = invoiceRepository.sumTotalByContractIdInAndStatus(contractIds, InvoiceStatus.PAID);
            totalPaid = total != null ? total : BigDecimal.ZERO;
        }

        BigDecimal currentDue = BigDecimal.ZERO;
        long overdueCount = 0;
        if (!unpaid.isEmpty()) {
            Map<Long, BigDecimal> paidByInvoice = paidTotalsByInvoice(
                    unpaid.stream().map(Invoice::getId).toList());
            for (Invoice invoice : unpaid) {
                BigDecimal due = invoice.getTotalAmount().subtract(invoice.getDiscount());
                due = due.subtract(paidByInvoice.getOrDefault(invoice.getId(), BigDecimal.ZERO))
                        .max(BigDecimal.ZERO);
                currentDue = currentDue.add(due);
                if (isOverdue(invoice)) {
                    overdueCount++;
                }
            }
        }

        TenantDashboardResponse.TenantDashboardTenant tenantInfo = new TenantDashboardTenant(
                tenant.getId(), tenantName(tenant));

        TenantDashboardRoom roomInfo = null;
        TenantDashboardContract contractInfo = null;
        if (contract != null) {
            contractInfo = new TenantDashboardContract(
                    contract.getId(),
                    contract.getContractNo(),
                    contract.getStartDate(),
                    contract.getEndDate(),
                    contract.getMonthlyRent(),
                    contract.getDueDay(),
                    contract.getStatus());
        }
        if (room != null) {
            roomInfo = new TenantDashboardRoom(
                    room.getId(),
                    house != null ? house.getName() : null,
                    room.getRoomNumber(),
                    room.getName());
        }

        return new TenantDashboardResponse(
                tenantInfo,
                roomInfo,
                contractInfo,
                new TenantDashboardFinancial(currentDue, totalPaid, unpaid.size(), overdueCount, paidCount),
                currentUtilities(room));
    }

    @Override
    @Transactional(readOnly = true)
    public TenantUtilityTrendResponse getUtilityUsage(YearMonth from, YearMonth to) {
        YearMonth start = from != null ? from : YearMonth.now().minusMonths(DEFAULT_RANGE_MONTHS - 1);
        YearMonth end = to != null ? to : YearMonth.now();
        validateRange(start, end);

        List<UtilityUsageResponse> water = new ArrayList<>();
        List<UtilityUsageResponse> electricity = new ArrayList<>();

        Tenant tenant = requireCurrentTenant();
        RentalContract contract = tenantContextResolver.activeContractOf(tenant.getId());
        if (contract == null) {
            return new TenantUtilityTrendResponse(water, electricity);
        }
        Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
        if (room == null) {
            return new TenantUtilityTrendResponse(water, electricity);
        }

        List<UtilityMeter> meters = utilityMeterRepository.findByRoomId(room.getId()).stream()
                .filter(meter -> meter.getStatus() == UtilityMeterStatus.ACTIVE)
                .toList();
        if (meters.isEmpty()) {
            return new TenantUtilityTrendResponse(water, electricity);
        }

        Map<Long, UtilityBucket> bucketByMeter = new HashMap<>();
        Map<Long, String> unitByMeter = new HashMap<>();
        for (UtilityMeter meter : meters) {
            UtilityType type = meter.getUtilityTypeId() != null
                    ? utilityTypeRepository.findById(meter.getUtilityTypeId()).orElse(null)
                    : null;
            bucketByMeter.put(meter.getId(), bucketOf(type));
            unitByMeter.put(meter.getId(), type != null ? type.getUnit() : null);
        }

        List<Long> meterIds = meters.stream().map(UtilityMeter::getId).toList();
        List<UtilityReading> readings = utilityReadingRepository
                .findByMeterIdInAndReadingDateBetween(meterIds, start.atDay(1), end.atEndOfMonth());

        Map<Long, Map<YearMonth, UtilityReading>> latestPerMeterMonth = new HashMap<>();
        for (UtilityReading reading : readings) {
            UtilityBucket bucket = bucketByMeter.get(reading.getMeter().getId());
            if (bucket == null || reading.getUsage() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(reading.getReadingDate());
            if (month.isBefore(start) || month.isAfter(end)) {
                continue;
            }
            latestPerMeterMonth.computeIfAbsent(reading.getMeter().getId(), key -> new HashMap<>())
                    .merge(month, reading, (current, next) ->
                            next.getReadingDate().isAfter(current.getReadingDate()) ? next : current);
        }

        Map<String, Map<UtilityBucket, BigDecimal>> totalsByMonth = new TreeMap<>();
        for (UtilityMeter meter : meters) {
            UtilityBucket bucket = bucketByMeter.get(meter.getId());
            latestPerMeterMonth.getOrDefault(meter.getId(), Map.of()).forEach((month, reading) ->
                    totalsByMonth
                            .computeIfAbsent(monthLabel(month), key -> new EnumMap<>(UtilityBucket.class))
                            .merge(bucket, reading.getUsage(), BigDecimal::add));
        }

        Map<UtilityBucket, String> unitByBucket = new EnumMap<>(UtilityBucket.class);
        for (UtilityMeter meter : meters) {
            UtilityBucket bucket = bucketByMeter.get(meter.getId());
            if (bucket != null && unitByBucket.get(bucket) == null) {
                unitByBucket.put(bucket, unitByMeter.get(meter.getId()));
            }
        }

        for (Map.Entry<String, Map<UtilityBucket, BigDecimal>> entry : totalsByMonth.entrySet()) {
            BigDecimal waterUsage = entry.getValue().get(UtilityBucket.WATER);
            if (waterUsage != null) {
                water.add(new UtilityUsageResponse(entry.getKey(), waterUsage,
                        unitByBucket.get(UtilityBucket.WATER)));
            }
            BigDecimal electricityUsage = entry.getValue().get(UtilityBucket.ELECTRICITY);
            if (electricityUsage != null) {
                electricity.add(new UtilityUsageResponse(entry.getKey(), electricityUsage,
                        unitByBucket.get(UtilityBucket.ELECTRICITY)));
            }
        }

        return new TenantUtilityTrendResponse(water, electricity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantPaymentHistoryResponse> getPaymentHistory(YearMonth from, YearMonth to) {
        YearMonth start = from != null ? from : YearMonth.now().minusMonths(DEFAULT_RANGE_MONTHS - 1);
        YearMonth end = to != null ? to : YearMonth.now();
        validateRange(start, end);

        Tenant tenant = requireCurrentTenant();
        List<Long> contractIds = tenantContractIds(tenant.getId());
        if (contractIds.isEmpty()) {
            return List.of();
        }

        List<Invoice> invoices = invoiceRepository.findByContractIdIn(contractIds);
        if (invoices.isEmpty()) {
            return List.of();
        }

        List<Long> invoiceIds = invoices.stream().map(Invoice::getId).toList();
        List<Payment> payments = paymentRepository.findByInvoiceIdInAndStatus(invoiceIds, PaymentStatus.PAID);
        Map<Long, BigDecimal> paidByInvoice = new HashMap<>();
        Map<Long, Set<YearMonth>> paidMonthsByInvoice = new HashMap<>();
        for (Payment payment : payments) {
            paidByInvoice.merge(payment.getInvoiceId(), payment.getAmount(), BigDecimal::add);
            YearMonth paidMonth = paymentMonth(payment);
            if (paidMonth != null) {
                paidMonthsByInvoice
                        .computeIfAbsent(payment.getInvoiceId(), key -> new HashSet<>())
                        .add(paidMonth);
            }
        }

        Map<YearMonth, MonthAggregate> byMonth = new TreeMap<>();
        for (Invoice invoice : invoices) {
            YearMonth billingMonth = YearMonth.of(invoice.getBillingYear(), invoice.getBillingMonth());
            boolean billedInRange = withinRange(billingMonth, start, end);
            boolean paidInRange = paidMonthsByInvoice.getOrDefault(invoice.getId(), Set.of())
                    .stream()
                    .anyMatch(month -> withinRange(month, start, end));
            if (!billedInRange && !paidInRange) {
                continue;
            }
            MonthAggregate aggregate = byMonth.computeIfAbsent(billingMonth, key -> new MonthAggregate());
            aggregate.due = aggregate.due
                    .add(invoice.getTotalAmount().subtract(invoice.getDiscount()));
            aggregate.paid = aggregate.paid
                    .add(paidByInvoice.getOrDefault(invoice.getId(), BigDecimal.ZERO));
            aggregate.invoices.add(invoice);
        }

        List<TenantPaymentHistoryResponse> result = new ArrayList<>();
        for (Map.Entry<YearMonth, MonthAggregate> entry : byMonth.entrySet()) {
            result.add(new TenantPaymentHistoryResponse(
                    monthLabel(entry.getKey()),
                    entry.getValue().due,
                    resolveMonthStatus(entry.getValue())));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TenantInvoiceResponse> getMyInvoices(int page, int size) {
        if (page < 0) {
            throw new InvalidTenantQueryException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidTenantQueryException("size must be between 1 and 100");
        }

        Tenant tenant = requireCurrentTenant();
        List<Long> contractIds = tenantContractIds(tenant.getId());

        Page<Invoice> result;
        if (contractIds.isEmpty()) {
            result = new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
        } else {
            Sort sort = Sort.by(Sort.Direction.DESC, "billingYear")
                    .and(Sort.by(Sort.Direction.DESC, "billingMonth"))
                    .and(Sort.by(Sort.Direction.DESC, "id"));
            result = invoiceRepository.findByContractIdIn(contractIds,
                    PageRequest.of(page, size, sort));
        }

        List<TenantInvoiceResponse> content = result.getContent().stream()
                .map(tenantDashboardMapper::toListResponse)
                .toList();
        return PageResponse.of(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public TenantInvoiceDetailResponse getMyInvoice(Long invoiceId) {
        Tenant tenant = requireCurrentTenant();
        List<Long> contractIds = tenantContractIds(tenant.getId());

        Invoice invoice = contractIds.isEmpty() ? null
                : invoiceRepository.findByIdAndContractIdIn(invoiceId, contractIds).orElse(null);
        if (invoice == null) {
            throw InvoiceNotFoundException.forId(invoiceId);
        }

        RentalContract contract = rentalContractRepository.findById(invoice.getContractId()).orElse(null);
        Room room = contract != null ? roomRepository.findById(contract.getRoomId()).orElse(null) : null;
        House house = room != null ? houseRepository.findById(room.getHouseId()).orElse(null) : null;

        return tenantDashboardMapper.toDetailResponse(
                invoice,
                house != null ? house.getName() : null,
                room != null ? room.getRoomNumber() : null,
                unitsByItemType(room));
    }

    private Tenant requireCurrentTenant() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_TENANT)) {
            throw new AccessDeniedException("Only tenants can access this resource.");
        }
        User me = securityUtils.getCurrentUser();
        return tenantContextResolver.requireLinkedTenant(me.getId());
    }

    private List<Long> tenantContractIds(Long tenantId) {
        return contractTenantRepository.findByTenantId(tenantId).stream()
                .map(ContractTenant::getContractId)
                .toList();
    }

    private TenantDashboardUtilities currentUtilities(Room room) {
        UtilityUsageResponse water = null;
        UtilityUsageResponse electricity = null;
        if (room == null) {
            return new TenantDashboardUtilities(null, null);
        }
        for (UtilityMeter meter : utilityMeterRepository.findByRoomId(room.getId())) {
            if (meter.getStatus() != UtilityMeterStatus.ACTIVE) {
                continue;
            }
            UtilityBucket bucket = bucketOf(meterUtilities(meter));
            if (bucket == null) {
                continue;
            }
            Optional<UtilityReading> latest = utilityReadingRepository
                    .findTopByMeterIdOrderByReadingDateDesc(meter.getId());
            if (latest.isEmpty() || latest.get().getUsage() == null) {
                continue;
            }
            UtilityReading reading = latest.get();
            String unit = meterUtilities(meter) != null ? meterUtilities(meter).getUnit() : null;
            UtilityUsageResponse usage = new UtilityUsageResponse(
                    monthLabel(YearMonth.from(reading.getReadingDate())),
                    reading.getUsage(),
                    unit);
            if (bucket == UtilityBucket.WATER && water == null) {
                water = usage;
            } else if (bucket == UtilityBucket.ELECTRICITY && electricity == null) {
                electricity = usage;
            }
        }
        return new TenantDashboardUtilities(water, electricity);
    }

    private Map<InvoiceItemType, String> unitsByItemType(Room room) {
        Map<InvoiceItemType, String> units = new EnumMap<>(InvoiceItemType.class);
        if (room == null) {
            return units;
        }
        for (UtilityMeter meter : utilityMeterRepository.findByRoomId(room.getId())) {
            if (meter.getStatus() != UtilityMeterStatus.ACTIVE) {
                continue;
            }
            UtilityType type = meterUtilities(meter);
            if (type == null) {
                continue;
            }
            InvoiceItemType itemType = itemTypeOf(type);
            if (itemType != null) {
                units.putIfAbsent(itemType, type.getUnit());
            }
        }
        return units;
    }

    private UtilityType meterUtilities(UtilityMeter meter) {
        return meter.getUtilityTypeId() != null
                ? utilityTypeRepository.findById(meter.getUtilityTypeId()).orElse(null)
                : null;
    }

    private void validateRange(YearMonth start, YearMonth end) {
        if (start.isAfter(end)) {
            throw new InvalidTenantQueryException("'from' must be before or equal to 'to'");
        }
    }

    private InvoiceStatus resolveMonthStatus(MonthAggregate aggregate) {
        if (aggregate.paid.compareTo(aggregate.due) >= 0) {
            return InvoiceStatus.PAID;
        }
        if (aggregate.paid.signum() > 0) {
            return InvoiceStatus.PARTIAL;
        }
        if (aggregate.invoices.stream().anyMatch(this::isOverdue)) {
            return InvoiceStatus.OVERDUE;
        }
        if (aggregate.invoices.stream().anyMatch(invoice ->
                invoice.getStatus() == InvoiceStatus.PENDING
                        || invoice.getStatus() == InvoiceStatus.PARTIAL
                        || invoice.getStatus() == InvoiceStatus.DRAFT)) {
            return InvoiceStatus.PENDING;
        }
        return InvoiceStatus.CANCELLED;
    }

    private boolean isOverdue(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.OVERDUE) {
            return true;
        }
        if (invoice.getStatus() != InvoiceStatus.PENDING
                && invoice.getStatus() != InvoiceStatus.PARTIAL) {
            return false;
        }
        return invoice.getDueDate() != null
                && invoice.getDueDate().isBefore(LocalDate.now());
    }

    private Map<Long, BigDecimal> paidTotalsByInvoice(List<Long> invoiceIds) {
        Map<Long, BigDecimal> paid = new HashMap<>();
        if (invoiceIds.isEmpty()) {
            return paid;
        }
        for (Object[] row : paymentRepository.sumPaidAmountByInvoiceIds(invoiceIds, PaymentStatus.PAID)) {
            paid.put((Long) row[0], (BigDecimal) row[1]);
        }
        return paid;
    }

    private static boolean withinRange(YearMonth month, YearMonth start, YearMonth end) {
        return !month.isBefore(start) && !month.isAfter(end);
    }

    private static YearMonth paymentMonth(Payment payment) {
        LocalDateTime instant = payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt();
        return instant != null ? YearMonth.from(instant.toLocalDate()) : null;
    }

    private static String monthLabel(YearMonth month) {
        return String.format("%04d-%02d", month.getYear(), month.getMonthValue());
    }

    private static String tenantName(Tenant tenant) {
        String first = tenant.getFirstName() == null ? "" : tenant.getFirstName();
        String last = tenant.getLastName() == null ? "" : tenant.getLastName();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? tenant.getTenantCode() : name;
    }

    private static UtilityBucket bucketOf(UtilityType type) {
        if (type == null) {
            return null;
        }
        String code = type.getCode() != null ? type.getCode().toUpperCase() : "";
        return switch (code) {
            case "WATER", "WTR" -> UtilityBucket.WATER;
            case "ELECTRICITY", "ELECTRIC", "ELECTRICITY_USAGE", "ELEC" -> UtilityBucket.ELECTRICITY;
            default -> bucketByUnit(type.getUnit());
        };
    }

    private static UtilityBucket bucketByUnit(String unit) {
        if (unit == null) {
            return null;
        }
        String normalized = unit.toLowerCase(Locale.ROOT);
        if (normalized.contains("kwh")) {
            return UtilityBucket.ELECTRICITY;
        }
        if (normalized.contains("m3") || normalized.contains("m\u00B3") || normalized.contains("cubic")) {
            return UtilityBucket.WATER;
        }
        return null;
    }

    private static InvoiceItemType itemTypeOf(UtilityType type) {
        if (type == null) {
            return null;
        }
        String code = type.getCode() != null ? type.getCode().toUpperCase() : "";
        InvoiceItemType byCode = switch (code) {
            case "ELECTRICITY", "ELECTRIC", "ELECTRICITY_USAGE", "ELEC" -> InvoiceItemType.ELECTRICITY;
            case "WATER", "WTR" -> InvoiceItemType.WATER;
            case "INTERNET" -> InvoiceItemType.INTERNET;
            case "PARKING" -> InvoiceItemType.PARKING;
            default -> null;
        };
        if (byCode != null) {
            return byCode;
        }
        UtilityBucket bucket = bucketByUnit(type.getUnit());
        if (bucket == UtilityBucket.WATER) {
            return InvoiceItemType.WATER;
        }
        if (bucket == UtilityBucket.ELECTRICITY) {
            return InvoiceItemType.ELECTRICITY;
        }
        return null;
    }

    private enum UtilityBucket {
        WATER, ELECTRICITY
    }

    private static class MonthAggregate {
        private BigDecimal due = BigDecimal.ZERO;
        private BigDecimal paid = BigDecimal.ZERO;
        private final List<Invoice> invoices = new ArrayList<>();
    }
}