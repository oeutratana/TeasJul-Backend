package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.AddInvoiceItemRequest;
import com.example.spring_boot_project_api.dto.request.BulkGenerateInvoiceRequest;
import com.example.spring_boot_project_api.dto.request.GenerateInvoiceRequest;
import com.example.spring_boot_project_api.dto.response.BulkGenerateInvoiceResponse;
import com.example.spring_boot_project_api.dto.response.ContractEligibleResponse;
import com.example.spring_boot_project_api.dto.response.GenerateBulkPreviewResponse;
import com.example.spring_boot_project_api.dto.response.InvoiceItemResponse;
import com.example.spring_boot_project_api.dto.response.InvoicePreviewResponse;
import com.example.spring_boot_project_api.dto.response.InvoiceResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.enums.InvoiceItemType;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.UtilityBillingMethod;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.enums.UtilityRateStatus;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.InvalidInvoiceOperationException;
import com.example.spring_boot_project_api.exception.InvoiceNotFoundException;
import com.example.spring_boot_project_api.exception.RentalContractNotFoundException;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.InvoiceMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.InvoiceItem;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomUtility;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityMeter;
import com.example.spring_boot_project_api.model.UtilityRate;
import com.example.spring_boot_project_api.model.UtilityReading;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomUtilityRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityRateRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.InvoiceService;
import com.example.spring_boot_project_api.service.NotificationService;
import com.example.spring_boot_project_api.service.OwnerSettingService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.InvoiceBalanceCalculator;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceMapper invoiceMapper;
    private final RentalContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final ContractTenantRepository contractTenantRepository;
    private final TenantRepository tenantRepository;
    private final UtilityMeterRepository meterRepository;
    private final UtilityRateRepository rateRepository;
    private final UtilityTypeRepository typeRepository;
    private final RoomUtilityRepository roomUtilityRepository;
    private final UserRepository userRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final SecurityUtils securityUtils;
    private final HouseAccessService houseAccessService;
    private final NotificationService notificationService;
    private final OwnerSettingService ownerSettingService;

    // ---- Preview ------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public InvoicePreviewResponse previewInvoice(GenerateInvoiceRequest request) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_CREATE);
        ContractBilling ctx = buildBillingContext(request);
        List<InvoiceItemResponse> items = computeItems(ctx, request.billingYear(), request.billingMonth());
        BigDecimal subtotal = itemsSubtotal(items);
        BigDecimal total = subtotal.subtract(ctx.discount());

        return new InvoicePreviewResponse(
                ctx.contract().getId(),
                ctx.contract().getContractNo(),
                ctx.room().getId(),
                ctx.room().getRoomNumber(),
                ctx.house().getId(),
                ctx.house().getName(),
                ctx.primaryTenant() != null ? ctx.primaryTenant().getId() : null,
                ctx.primaryTenant() != null
                        ? ctx.primaryTenant().getFirstName() + " " + ctx.primaryTenant().getLastName()
                        : null,
                request.billingYear(),
                request.billingMonth(),
                resolveDueDate(request, ctx.contract(), ctx.owner().getId()),
                items,
                subtotal,
                ctx.discount(),
                total);
    }

    // ---- Generate -----------------------------------------------------

    @Override
    @Transactional
    public InvoiceResponse generateMonthlyInvoice(GenerateInvoiceRequest request) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_CREATE);
        ContractBilling ctx = buildBillingContext(request);

        if (invoiceRepository.existsByContractIdAndBillingYearAndBillingMonth(
                ctx.contract().getId(), request.billingYear(), request.billingMonth())) {
            throw new DuplicateResourceException(
                    "An invoice already exists for this contract for "
                            + YearMonth.of(request.billingYear(), request.billingMonth()));
        }

        List<InvoiceItemResponse> items = computeItems(ctx, request.billingYear(), request.billingMonth());
        BigDecimal subtotal = itemsSubtotal(items);
        LocalDate dueDate = resolveDueDate(request, ctx.contract(), ctx.owner().getId());
        // Policy late fee: a flat amount per calendar day past the due date, stored on the invoice.
        BigDecimal lateFee = InvoiceBalanceCalculator.defaultLateFee(dueDate, LocalDate.now());
        BigDecimal total = InvoiceBalanceCalculator.invoiceTotal(subtotal, ctx.discount(), lateFee);

        Invoice invoice = Invoice.builder()
                .owner(ctx.owner())
                .invoiceNo(generateInvoiceNo(ctx.owner().getId(), request.billingYear(), request.billingMonth()))
                .contractId(ctx.contract().getId())
                .billingYear(request.billingYear())
                .billingMonth(request.billingMonth())
                .dueDate(dueDate)
                .subtotal(subtotal)
                .discount(ctx.discount())
                .lateFee(lateFee)
                .totalAmount(total)
                .exchangeRate(ownerSettingService.getExchangeRate(ctx.owner().getId()))
                .exchangeRateCurrency("KHR")
                .status(InvoiceStatus.PENDING)
                .build();

        for (InvoiceItemResponse item : items) {
            invoice.addItem(InvoiceItem.builder()
                    .itemType(item.itemType())
                    .description(item.description())
                    .quantity(item.quantity())
                    .unitPrice(item.unitPrice())
                    .amount(item.amount())
                    .build());
        }

        Invoice saved = invoiceRepository.save(invoice);
        try {
            notificationService.notifyInvoiceCreated(saved);
        } catch (RuntimeException e) {
            log.warn("Failed to create invoice notification", e);
        }
        return toResponse(saved, ctx.details());
    }

    // ---- Bulk Generate ------------------------------------------------

    @Override
    @Transactional
    public BulkGenerateInvoiceResponse generateBulkInvoices(BulkGenerateInvoiceRequest request) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_CREATE);
        verifyOwnerOrManagerRole();

        List<Long> houseIds = resolveEligibleHouseScope(request.houseId());
        List<RentalContract> activeContracts = houseIds.isEmpty() ? List.of()
                : contractRepository.findByRoomHouseIdInAndStatus(houseIds, RentalContractStatus.ACTIVE);

        List<InvoiceResponse> createdInvoices = new ArrayList<>();
        List<String> skippedReasons = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (RentalContract contract : activeContracts) {
            try {
                if (invoiceRepository.existsByContractIdAndBillingYearAndBillingMonth(
                        contract.getId(), request.billingYear(), request.billingMonth())) {
                    skippedReasons.add("Invoice already exists for contract " + contract.getContractNo()
                            + " for " + YearMonth.of(request.billingYear(), request.billingMonth()));
                    continue;
                }

                GenerateInvoiceRequest singleRequest = new GenerateInvoiceRequest(
                        contract.getId(),
                        request.billingYear(),
                        request.billingMonth(),
                        request.discount(),
                        request.dueDate());

                InvoiceResponse created = generateMonthlyInvoice(singleRequest);
                createdInvoices.add(created);
            } catch (Exception e) {
                errors.add("Failed to generate invoice for contract " + contract.getContractNo() + ": " + e.getMessage());
            }
        }

        return new BulkGenerateInvoiceResponse(
                createdInvoices.size(),
                skippedReasons.size(),
                errors.size(),
                createdInvoices,
                skippedReasons,
                errors);
    }

    // ---- Invoice generation eligibility --------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContractEligibleResponse> getEligibleContracts(Long houseId, String search,
            Integer billingYear, Integer billingMonth, Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_CREATE);
        verifyOwnerOrManagerRole();

        List<Long> houseIds = resolveEligibleHouseScope(houseId);
        if (houseIds.isEmpty()) {
            return PageResponse.of(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
        }

        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        Page<RentalContract> page = contractRepository.findEligibleForInvoice(
                houseIds, normalizedSearch, RentalContractStatus.ACTIVE, pageable);

        List<RentalContract> contracts = page.getContent();
        List<Long> contractIds = contracts.stream()
                .map(RentalContract::getId)
                .filter(Objects::nonNull)
                .toList();

        Set<Long> invoicedContractIds = new HashSet<>();
        if (billingYear != null && billingMonth != null && !contractIds.isEmpty()) {
            invoicedContractIds.addAll(invoiceRepository.findInvoicedContractIds(
                    contractIds, billingYear, billingMonth));
        }

        List<Long> roomIds = contracts.stream()
                .map(RentalContract::getRoomId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Room> rooms = roomIds.isEmpty() ? Map.of()
                : roomRepository.findAllById(roomIds).stream()
                        .collect(Collectors.toMap(Room::getId, r -> r));

        List<Long> rowHouseIds = rooms.values().stream()
                .map(Room::getHouseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, House> houses = rowHouseIds.isEmpty() ? Map.of()
                : houseRepository.findAllById(rowHouseIds).stream()
                        .collect(Collectors.toMap(House::getId, h -> h));

        Map<Long, ContractTenant> primaryLinks = primaryTenantLinks(contractIds);
        List<Long> tenantIds = primaryLinks.values().stream()
                .map(ContractTenant::getTenantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Tenant> tenants = tenantIds.isEmpty() ? Map.of()
                : tenantRepository.findAllById(tenantIds).stream()
                        .collect(Collectors.toMap(Tenant::getId, t -> t));

        List<ContractEligibleResponse> content = contracts.stream()
                .map(contract -> {
                    Room rowRoom = rooms.get(contract.getRoomId());
                    House rowHouse = rowRoom != null ? houses.get(rowRoom.getHouseId()) : null;
                    ContractTenant link = primaryLinks.get(contract.getId());
                    Tenant tenant = link != null ? tenants.get(link.getTenantId()) : null;
                    String tenantName = tenant != null
                            ? (tenant.getFirstName() + " " + tenant.getLastName()).trim()
                            : null;
                    return new ContractEligibleResponse(
                            contract.getId(),
                            contract.getContractNo(),
                            tenant != null ? tenant.getId() : null,
                            tenantName,
                            rowRoom != null ? rowRoom.getId() : null,
                            rowRoom != null ? rowRoom.getRoomNumber() : null,
                            rowHouse != null ? rowHouse.getId() : null,
                            rowHouse != null ? rowHouse.getName() : null,
                            contract.getMonthlyRent(),
                            contract.getStartDate(),
                            contract.getEndDate(),
                            invoicedContractIds.contains(contract.getId()));
                })
                .toList();

        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public GenerateBulkPreviewResponse previewBulkGeneration(Long houseId,
            Integer billingYear, Integer billingMonth) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_CREATE);
        verifyOwnerOrManagerRole();

        YearMonth period = resolveBillingPeriod(billingYear, billingMonth);
        List<Long> houseIds = resolveEligibleHouseScope(houseId);

        long active = 0L;
        long alreadyInvoiced = 0L;
        if (!houseIds.isEmpty()) {
            active = contractRepository.countActiveByHouseIds(houseIds, RentalContractStatus.ACTIVE);
            alreadyInvoiced = contractRepository.countAlreadyInvoicedByHouseIds(
                    houseIds, RentalContractStatus.ACTIVE, period.getYear(), period.getMonthValue());
        }
        long toGenerate = Math.max(active - alreadyInvoiced, 0L);

        return new GenerateBulkPreviewResponse(active, alreadyInvoiced, toGenerate,
                period.getMonthValue(), period.getYear());
    }

    // ---- List ---------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> getInvoices(Long houseId, String search, InvoiceStatus status,
            Integer billingYear, Integer billingMonth, Long tenantId, LocalDate fromDate,
            LocalDate toDate, Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_VIEW);
        Long currentUserId = securityUtils.getCurrentUserId();

        List<Long> accessibleHouseIds = houseAccessService.getAccessibleHouseIds();

        // Owners/managers without any house access must never see invoices that
        // belong to other owners' houses.
        boolean isAdminScope = !accessibleHouseIds.isEmpty();

        // A tenant that reaches this endpoint must only ever see the invoices of
        // their own linked tenant profile, never invoices belonging to other rents.
        Long tenantScopeId = null;
        if (!isAdminScope) {
            if (!securityUtils.hasRole(SecurityUtils.ROLE_TENANT)
                    || securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                    || securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
                return PageResponse.of(List.of(), pageable.getPageNumber(),
                        pageable.getPageSize(), 0, 0);
            }
            tenantScopeId = tenantRepository.findByUserId(currentUserId)
                    .map(Tenant::getId)
                    .orElse(null);
            if (tenantScopeId == null) {
                return PageResponse.of(List.of(), pageable.getPageNumber(),
                        pageable.getPageSize(), 0, 0);
            }
            if (tenantId != null && !tenantId.equals(tenantScopeId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You do not have permission to access this resource");
            }
        }

        List<Long> scopeHouseIds = List.of();
        if (isAdminScope) {
            scopeHouseIds = accessibleHouseIds;
            if (houseId != null) {
                houseAccessService.requireHouseAccess(houseId);
                scopeHouseIds = List.of(houseId);
            }
        }
        final List<Long> finalScopeHouseIds = scopeHouseIds;
        final Long finalTenantScopeId = tenantScopeId;
        final Long requestTenantId = isAdminScope ? tenantId : finalTenantScopeId;

        Specification<Invoice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Subquery<Long> contractSub = query.subquery(Long.class);
            Root<RentalContract> contractRoot = contractSub.from(RentalContract.class);

            if (isAdminScope) {
                Subquery<Long> roomSub = query.subquery(Long.class);
                Root<Room> roomRoot = roomSub.from(Room.class);
                roomSub.select(roomRoot.get("id"))
                        .where(roomRoot.get("houseId").in(finalScopeHouseIds));
                contractSub.select(contractRoot.get("id"))
                        .where(contractRoot.get("roomId").in(roomSub));
            } else {
                Subquery<Long> linkedContractSub = query.subquery(Long.class);
                Root<ContractTenant> linkRoot = linkedContractSub.from(ContractTenant.class);
                linkedContractSub.select(linkRoot.get("contractId"))
                        .where(cb.equal(linkRoot.get("tenantId"), finalTenantScopeId));
                contractSub.select(contractRoot.get("id"))
                        .where(contractRoot.get("id").in(linkedContractSub));
            }
            predicates.add(root.get("contractId").in(contractSub));

            if (requestTenantId != null) {
                Subquery<Long> tenantContractSub = query.subquery(Long.class);
                Root<ContractTenant> ctRoot = tenantContractSub.from(ContractTenant.class);
                tenantContractSub.select(ctRoot.get("contractId"))
                        .where(cb.equal(ctRoot.get("tenantId"), requestTenantId));
                predicates.add(root.get("contractId").in(tenantContractSub));
            }

            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (billingYear != null) predicates.add(cb.equal(root.get("billingYear"), billingYear));
            if (billingMonth != null) predicates.add(cb.equal(root.get("billingMonth"), billingMonth));
            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                List<Predicate> searchPreds = new ArrayList<>();
                searchPreds.add(cb.like(cb.lower(root.get("invoiceNo")), term));

                try {
                    Long searchId = Long.parseLong(search.trim());
                    searchPreds.add(cb.equal(root.get("id"), searchId));
                } catch (NumberFormatException ignored) {
                }

                Subquery<Long> tenantContractSub = query.subquery(Long.class);
                Root<ContractTenant> ctRoot = tenantContractSub.from(ContractTenant.class);
                Root<Tenant> tRoot = tenantContractSub.from(Tenant.class);

                Predicate ctLink = cb.equal(ctRoot.get("tenantId"), tRoot.get("id"));
                Predicate nameOrPhoneMatch = cb.or(
                        cb.like(cb.lower(tRoot.get("firstName")), term),
                        cb.like(cb.lower(tRoot.get("lastName")), term),
                        cb.like(cb.lower(cb.concat(cb.concat(tRoot.get("firstName"), " "), tRoot.get("lastName"))), term),
                        cb.like(cb.lower(cb.concat(cb.concat(tRoot.get("lastName"), " "), tRoot.get("firstName"))), term),
                        cb.like(cb.lower(tRoot.get("phone")), term)
                );
                tenantContractSub.select(ctRoot.get("contractId"))
                        .where(cb.and(ctLink, nameOrPhoneMatch));

                searchPreds.add(root.get("contractId").in(tenantContractSub));

                predicates.add(cb.or(searchPreds.toArray(new Predicate[0])));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate.atTime(23, 59, 59)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Invoice> page = invoiceRepository.findAll(spec, pageable);

        List<InvoiceResponse> content = toResponsesWithDetails(page.getContent());

        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private List<InvoiceResponse> toResponsesWithDetails(List<Invoice> invoices) {
        if (invoices == null || invoices.isEmpty()) {
            return List.of();
        }

        List<Long> invoiceIds = invoices.stream()
                .map(Invoice::getId)
                .filter(Objects::nonNull)
                .toList();
        List<Long> contractIds = invoices.stream()
                .map(Invoice::getContractId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Load all items of the page in one query instead of one lazy load per invoice.
        Map<Long, List<InvoiceItem>> itemsByInvoice = invoiceIds.isEmpty() ? Map.of()
                : invoiceRepository.findWithItemsByIds(invoiceIds).stream()
                        .collect(Collectors.toMap(Invoice::getId, Invoice::getItems));
        for (Invoice invoice : invoices) {
            invoice.setItems(itemsByInvoice.getOrDefault(invoice.getId(), List.of()));
        }

        Map<Long, RentalContract> contracts = contractIds.isEmpty() ? Map.of()
                : contractRepository.findAllById(contractIds).stream()
                        .collect(Collectors.toMap(RentalContract::getId, c -> c));

        List<Long> roomIds = contracts.values().stream()
                .map(RentalContract::getRoomId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Room> rooms = roomIds.isEmpty() ? Map.of()
                : roomRepository.findAllById(roomIds).stream()
                        .collect(Collectors.toMap(Room::getId, r -> r));

        List<Long> houseIds = rooms.values().stream()
                .map(Room::getHouseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, House> houses = houseIds.isEmpty() ? Map.of()
                : houseRepository.findAllById(houseIds).stream()
                        .collect(Collectors.toMap(House::getId, h -> h));

        Map<Long, ContractTenant> primaryLinks = primaryTenantLinks(contractIds);
        List<Long> tenantIds = primaryLinks.values().stream()
                .map(ContractTenant::getTenantId)
                .distinct()
                .toList();
        Map<Long, Tenant> tenants = tenantIds.isEmpty() ? Map.of()
                : tenantRepository.findAllById(tenantIds).stream()
                        .collect(Collectors.toMap(Tenant::getId, t -> t));
        Map<Long, BigDecimal> paidAmounts = paidAmountsByInvoiceId(invoiceIds);

        return invoices.stream()
                .map(invoice -> {
                    RentalContract contract = contracts.get(invoice.getContractId());
                    Room room = contract != null ? rooms.get(contract.getRoomId()) : null;
                    House house = room != null ? houses.get(room.getHouseId()) : null;
                    ContractTenant link = primaryLinks.get(invoice.getContractId());
                    Tenant tenant = link != null ? tenants.get(link.getTenantId()) : null;
                    Object[] details = new Object[] {
                            contract != null ? contract.getContractNo() : null,
                            tenant != null ? tenant.getId() : null,
                            tenant != null
                                    ? tenant.getFirstName() + " " + tenant.getLastName()
                                    : null,
                            room != null ? room.getId() : null,
                            room != null ? room.getRoomNumber() : null,
                            house != null ? house.getId() : null,
                            house != null ? house.getName() : null
                    };
                    return invoiceMapper.toResponse(invoice, details,
                            paidAmounts.getOrDefault(invoice.getId(), BigDecimal.ZERO));
                })
                .toList();
    }

    private Map<Long, ContractTenant> primaryTenantLinks(List<Long> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<ContractTenant>> linksByContract = contractTenantRepository
                .findByContractIdIn(List.copyOf(contractIds)).stream()
                .collect(Collectors.groupingBy(ContractTenant::getContractId));

        Map<Long, ContractTenant> result = new HashMap<>();
        for (Map.Entry<Long, List<ContractTenant>> entry : linksByContract.entrySet()) {
            ContractTenant chosen = entry.getValue().stream()
                    .filter(l -> Boolean.TRUE.equals(l.getIsPrimary()))
                    .findFirst()
                    .orElseGet(() -> entry.getValue().stream()
                            .min(Comparator.comparing(ContractTenant::getJoinedAt)
                                    .thenComparing(ContractTenant::getId))
                            .orElse(null));
            if (chosen != null) {
                result.put(entry.getKey(), chosen);
            }
        }
        return result;
    }

    private InvoiceResponse toResponse(Invoice invoice, Object[] details) {
        BigDecimal paidAmount = paymentRepository.findPaidTotalByInvoiceId(invoice.getId());
        return invoiceMapper.toResponse(invoice, details, paidAmount);
    }

    /** Aggregates PAID records once for an invoice list; pending and failed attempts are excluded in SQL. */
    private Map<Long, BigDecimal> paidAmountsByInvoiceId(List<Long> invoiceIds) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BigDecimal> paidAmounts = new HashMap<>();
        List<Object[]> rows = paymentRepository.sumPaidAmountByInvoiceIds(invoiceIds, PaymentStatus.PAID);
        if (rows == null) {
            return paidAmounts;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            Long invoiceId = ((Number) row[0]).longValue();
            BigDecimal amount = row[1] instanceof BigDecimal value
                    ? value : new BigDecimal(row[1].toString());
            paidAmounts.put(invoiceId, amount);
        }
        return paidAmounts;
    }

    // ---- Get / modify -------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_VIEW);
        Invoice invoice = getAccessibleInvoice(id);
        return toResponse(invoice, resolveDetails(invoice));
    }

    @Override
    @Transactional
    public InvoiceResponse addItem(Long id, AddInvoiceItemRequest request) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_UPDATE);
        Invoice invoice = getAccessibleInvoice(id);
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidInvoiceOperationException(
                    "Cannot modify an invoice that is " + invoice.getStatus());
        }
        if (request.itemType() == null) {
            throw new InvalidInvoiceOperationException("Item type is required");
        }

        InvoiceItem item = InvoiceItem.builder()
                .invoice(invoice)
                .itemType(request.itemType())
                .description(request.description())
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .build();
        item.calculateAmount();
        invoice.addItem(item);

        recomputeTotals(invoice);
        Invoice saved = invoiceRepository.save(invoice);
        return toResponse(saved, resolveDetails(saved));
    }

    @Override
    @Transactional
    public InvoiceResponse updateStatus(Long id, InvoiceStatus status) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_UPDATE);
        if (status == InvoiceStatus.CANCELLED) {
            cancelInvoice(id);
            return getInvoiceById(id);
        }
        Invoice invoice = getAccessibleInvoice(id);
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidInvoiceOperationException("A cancelled invoice cannot be modified");
        }
        if (status == InvoiceStatus.PENDING || status == InvoiceStatus.PARTIAL || status == InvoiceStatus.PAID) {
            BigDecimal paidTotal = paymentRepository.findPaidTotalByInvoiceId(invoice.getId());
            invoice.setStatus(InvoiceBalanceCalculator.paymentStatus(invoice.getTotalAmount(), paidTotal));
        } else {
            invoice.setStatus(status);
        }
        Invoice saved = invoiceRepository.save(invoice);
        return toResponse(saved, resolveDetails(saved));
    }

    @Override
    @Transactional
    public void cancelInvoice(Long id) {
        Invoice invoice = getAccessibleInvoice(id);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidInvoiceOperationException("A paid invoice cannot be cancelled");
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse markPaid(Long id) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_UPDATE);
        Invoice invoice = getAccessibleInvoice(id);
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidInvoiceOperationException("A cancelled invoice cannot receive payment");
        }

        BigDecimal paidTotal = paymentRepository.findPaidTotalByInvoiceId(invoice.getId());
        invoice.setStatus(InvoiceBalanceCalculator.paymentStatus(invoice.getTotalAmount(), paidTotal));
        Invoice saved = invoiceRepository.save(invoice);
        return toResponse(saved, resolveDetails(saved));
    }

    @Override
    @Transactional
    public InvoiceResponse recordPayment(Long id, BigDecimal paidAmount) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_MANAGE);
        Invoice invoice = getAccessibleInvoice(id);
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidInvoiceOperationException("A cancelled invoice cannot receive payment");
        }
        if (paidAmount == null || paidAmount.signum() <= 0) {
            throw new InvalidInvoiceOperationException("Payment amount must be positive");
        }

        BigDecimal paidTotal = paymentRepository.findPaidTotalByInvoiceId(invoice.getId());
        invoice.setStatus(InvoiceBalanceCalculator.paymentStatus(invoice.getTotalAmount(), paidTotal));
        Invoice saved = invoiceRepository.save(invoice);
        return toResponse(saved, resolveDetails(saved));
    }

    // ---- Late fee -----------------------------------------------------

    @Override
    @Transactional
    public InvoiceResponse updateLateFee(Long id, BigDecimal lateFee) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_UPDATE);
        if (lateFee == null || lateFee.signum() < 0) {
            throw new InvalidInvoiceOperationException("Late fee must not be negative");
        }
        Invoice invoice = getAccessibleInvoice(id);
        requireLateFeeEditable(invoice);
        invoice.setLateFee(lateFee.setScale(2, RoundingMode.HALF_UP));
        applyLateFeeTotals(invoice);
        Invoice saved = invoiceRepository.save(invoice);
        return toResponse(saved, resolveDetails(saved));
    }

    @Override
    @Transactional
    public InvoiceResponse recalculateLateFee(Long id) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_UPDATE);
        Invoice invoice = getAccessibleInvoice(id);
        // Always derived from the due date, never incremented, so repeating this is a no-op.
        applyDefaultLateFee(invoice, LocalDate.now());
        applyLateFeeTotals(invoice);
        Invoice saved = invoiceRepository.save(invoice);
        return toResponse(saved, resolveDetails(saved));
    }

    /** A paid or cancelled invoice is closed: its stored late fee is left untouched. */
    private void requireLateFeeEditable(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidInvoiceOperationException("A paid invoice cannot have its late fee changed");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidInvoiceOperationException("A cancelled invoice cannot have its late fee changed");
        }
    }

    private void applyDefaultLateFee(Invoice invoice, LocalDate asOf) {
        if (!InvoiceBalanceCalculator.isLateFeeEligible(invoice.getStatus())) {
            return;
        }
        invoice.setLateFee(InvoiceBalanceCalculator.defaultLateFee(invoice.getDueDate(), asOf));
    }

    /**
     * Rebuilds the persisted total from the untouched subtotal and discount, then re-derives
     * the payment status from PAID payments only. The original subtotal and discount are
     * never modified by a late-fee change.
     */
    private void applyLateFeeTotals(Invoice invoice) {
        recomputeTotals(invoice);
        BigDecimal paidTotal = paymentRepository.findPaidTotalByInvoiceId(invoice.getId());
        invoice.setStatus(InvoiceBalanceCalculator.paymentStatus(invoice.getTotalAmount(), paidTotal));
    }

    // ---- Billing context ----------------------------------------------

    private record ContractBilling(
            User owner,
            RentalContract contract,
            Room room,
            House house,
            Tenant primaryTenant,
            List<RoomUtility> roomUtilities,
            BigDecimal discount,
            Object[] details) {
    }

    private ContractBilling buildBillingContext(GenerateInvoiceRequest request) {
        User owner = getCurrentOwner();
        Long ownerId = owner.getId();
        verifyOwnerOrManagerRole();

        RentalContract contract = contractRepository.findById(request.contractId())
                .orElseThrow(() -> RentalContractNotFoundException.forId(request.contractId()));

        if (!contract.getOwnerId().equals(ownerId)
                && !(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)
                        && canManageContract(contract))) {
            throw RentalContractNotFoundException.forId(request.contractId());
        }

        if (contract.getStatus() != RentalContractStatus.ACTIVE) {
            throw new InvalidInvoiceOperationException(
                    "Invoice can only be generated for an ACTIVE contract (current status: "
                            + contract.getStatus() + ")");
        }

        Room room = roomRepository.findById(contract.getRoomId())
                .orElseThrow(() -> RoomNotFoundException.forId(contract.getRoomId()));

        House house = houseRepository.findById(room.getHouseId())
                .orElseThrow(() -> HouseNotFoundException.forId(room.getHouseId()));

        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)
                && !userHouseAccessRepository.existsByUserIdAndHouseId(
                        securityUtils.getCurrentUserId(), house.getId())) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }

        // Records created by a manager belong to the actual house owner so the
        // owner always sees everything made by managers for their houses.
        if (!owner.getId().equals(house.getOwner().getId())) {
            owner = house.getOwner();
        }

        Tenant primaryTenant = primaryTenantOf(contract.getId());

        List<RoomUtility> roomUtilities = roomUtilityRepository
                .findByRoomIdAndStatus(room.getId(), UtilityTypeStatus.ACTIVE);

        BigDecimal discount = request.discount() != null ? request.discount() : BigDecimal.ZERO;

        Object[] details = new Object[] {
                contract.getContractNo(),
                primaryTenant != null ? primaryTenant.getId() : null,
                primaryTenant != null
                        ? primaryTenant.getFirstName() + " " + primaryTenant.getLastName()
                        : null,
                room.getId(),
                room.getRoomNumber(),
                house.getId(),
                house.getName()
        };

        return new ContractBilling(owner, contract, room, house, primaryTenant,
                roomUtilities, discount, details);
    }

    private boolean canManageContract(RentalContract contract) {
        Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
        if (room == null) return false;
        return userHouseAccessRepository.existsByUserIdAndHouseId(
                securityUtils.getCurrentUserId(), room.getHouseId());
    }

    private List<InvoiceItemResponse> computeItems(ContractBilling ctx, int billingYear, int billingMonth) {
        List<InvoiceItemResponse> items = new ArrayList<>();

        // Rent.
        BigDecimal rent = ctx.contract().getMonthlyRent().setScale(2, RoundingMode.HALF_UP);
        items.add(new InvoiceItemResponse(null, null, InvoiceItemType.RENT,
                "Room Rent", BigDecimal.ONE, rent, rent));

        // Utilities for the billing month, driven by the room's assigned
        // utility types (METERED: measured/readings; FIXED: flat monthly fee).
        YearMonth ym = YearMonth.of(billingYear, billingMonth);
        LocalDate periodStart = ym.atDay(1);
        LocalDate periodEnd = ym.atEndOfMonth();

        for (RoomUtility ru : ctx.roomUtilities()) {
            UtilityType type = typeRepository.findByIdAndOwnerId(
                    ru.getUtilityTypeId(), ctx.owner().getId())
                    .orElseThrow(() -> new InvalidInvoiceOperationException(
                            "Utility type " + ru.getUtilityTypeId()
                                    + " is no longer available; re-assign it for room "
                                    + ctx.room().getRoomNumber()));
            InvoiceItemType itemType = mapUtilityTypeToItemType(type);

            UtilityMeter meter = null;
            BigDecimal usage = BigDecimal.ZERO;
            BigDecimal unitPrice;
            BigDecimal amount;

            if (type.getBillingMethod() == UtilityBillingMethod.METERED) {
                meter = activeMeterForRoomAndType(ctx.room().getId(), type.getId(), ctx.owner().getId());
                if (meter == null) {
                    // No ACTIVE meter means no reading can be attributed to this
                    // room/type for the period, so no usage-based charge is
                    // invented: the utility is skipped instead of failing the invoice.
                    log.warn("Skipping metered utility {} for room {} in {}: "
                                    + "no ACTIVE meter is assigned",
                            type.getName(), ctx.room().getRoomNumber(), ym);
                    continue;
                }

                UtilityReading baseline = readingAtOrBefore(meter.getId(), periodStart.minusDays(1));
                UtilityReading periodReading = readingOnOrBefore(meter.getId(), periodEnd);

                if (periodReading == null) {
                    // No reading recorded by the end of the billing period: the
                    // usage cannot be computed, so the utility is skipped instead
                    // of failing the whole invoice.
                    log.warn("Skipping metered utility {} for room {} in {}: "
                                    + "no meter reading on or before {}",
                            type.getName(), ctx.room().getRoomNumber(), ym, periodEnd);
                    continue;
                }

                BigDecimal baselineValue = baseline != null ? baseline.getCurrentReading() : BigDecimal.ZERO;
                BigDecimal currentValue = periodReading.getCurrentReading();
                usage = currentValue.subtract(baselineValue);
                if (usage.signum() < 0) usage = BigDecimal.ZERO;
            }

            BigDecimal applicableRate = singleEffectiveRate(ctx.owner().getId(), ctx.house().getId(),
                    type.getId(), periodEnd);
            if (applicableRate == null) {
                throw new InvalidInvoiceOperationException(
                        "No applicable rate defined for " + type.getName()
                                + " on house " + ctx.house().getName()
                                + " effective " + periodEnd);
            }
            unitPrice = applicableRate.setScale(4, RoundingMode.HALF_UP);

            if (type.getBillingMethod() == UtilityBillingMethod.FIXED) {
                amount = unitPrice.setScale(2, RoundingMode.HALF_UP);
                items.add(new InvoiceItemResponse(null, null, itemType,
                        type.getName(), BigDecimal.ONE, unitPrice, amount));
            } else {
                amount = usage.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                items.add(new InvoiceItemResponse(null, null, itemType,
                        type.getName(), usage, unitPrice, amount));
            }
        }

        return items;
    }

    private UtilityMeter activeMeterForRoomAndType(Long roomId, Long utilityTypeId, Long ownerId) {
        return meterRepository.findByRoomIdAndUtilityTypeId(roomId, utilityTypeId).stream()
                .filter(m -> m.getStatus() == UtilityMeterStatus.ACTIVE)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal singleEffectiveRate(Long ownerId, Long houseId, Long utilityTypeId, LocalDate date) {
        List<UtilityRate> rates = invoiceRepository.findEffectiveRate(
                ownerId, houseId, utilityTypeId, date, UtilityRateStatus.ACTIVE);
        if (rates.isEmpty()) {
            return null;
        }
        if (rates.size() > 1
                && !rates.get(0).getEffectiveFrom().equals(rates.get(1).getEffectiveFrom())) {
            throw new InvalidInvoiceOperationException(
                    "Multiple applicable rates overlap for a utility on this house");
        }
        return rates.get(0).getRate();
    }

    private InvoiceItemType mapUtilityTypeToItemType(UtilityType type) {
        String code = type.getCode() != null ? type.getCode().toUpperCase() : "";
        switch (code) {
            case "ELECTRICITY": case "ELECTRIC": case "ELECTRICITY_USAGE":
                return InvoiceItemType.ELECTRICITY;
            case "WATER":
                return InvoiceItemType.WATER;
            case "INTERNET":
                return InvoiceItemType.INTERNET;
            case "PARKING":
                return InvoiceItemType.PARKING;
            default:
                return InvoiceItemType.OTHER;
        }
    }

    private Tenant primaryTenantOf(Long contractId) {
        List<ContractTenant> links = contractTenantRepository.findByContractId(contractId);
        ContractTenant chosen = null;
        for (ContractTenant link : links) {
            if (Boolean.TRUE.equals(link.getIsPrimary())) {
                chosen = link;
                break;
            }
        }
        if (chosen == null && !links.isEmpty()) {
            chosen = links.stream()
                    .min((a, b) -> {
                        int c = a.getJoinedAt().compareTo(b.getJoinedAt());
                        return c != 0 ? c : a.getId().compareTo(b.getId());
                    })
                    .orElse(null);
        }
        if (chosen == null) return null;
        return tenantRepository.findById(chosen.getTenantId()).orElse(null);
    }

    private Object[] resolveDetails(Invoice invoice) {
        RentalContract contract = contractRepository.findById(invoice.getContractId()).orElse(null);
        if (contract == null) return null;
        Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
        House house = room != null ? houseRepository.findById(room.getHouseId()).orElse(null) : null;
        Tenant tenant = primaryTenantOf(invoice.getContractId());
        return new Object[] {
                contract.getContractNo(),
                tenant != null ? tenant.getId() : null,
                tenant != null
                        ? tenant.getFirstName() + " " + tenant.getLastName()
                        : null,
                room != null ? room.getId() : null,
                room != null ? room.getRoomNumber() : null,
                house != null ? house.getId() : null,
                house != null ? house.getName() : null
        };
    }

    // ---- Helpers ------------------------------------------------------

    private Invoice getAccessibleInvoice(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        return invoiceRepository.findById(id)
                .map(invoice -> {
                    if (invoice.getOwner().getId().equals(currentUserId)) return invoice;
                    if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
                        RentalContract contract = contractRepository.findById(invoice.getContractId()).orElse(null);
                        if (contract != null) {
                            Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
                            if (room != null && userHouseAccessRepository.existsByUserIdAndHouseId(
                                    currentUserId, room.getHouseId())) {
                                return invoice;
                            }
                        }
                    }
                    throw new AccessDeniedException("You do not have permission to access this resource");
                })
                .orElseThrow(() -> InvoiceNotFoundException.forId(id));
    }

    private void recomputeTotals(Invoice invoice) {
        BigDecimal subtotal = invoice.getItems().stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setSubtotal(subtotal);
        invoice.setTotalAmount(subtotal.subtract(invoice.getDiscount()).add(invoice.getLateFee()));
    }

    private BigDecimal itemsSubtotal(List<InvoiceItemResponse> items) {
        return items.stream()
                .map(InvoiceItemResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateInvoiceNo(Long ownerId, int year, int month) {
        long seq = invoiceRepository.count() + 1;
        String prefix = ownerSettingService.getInvoicePrefix(ownerId);
        return String.format("%s-%04d-%04d", prefix, year, seq);
    }

    private LocalDate resolveDueDate(GenerateInvoiceRequest request, RentalContract contract, Long ownerId) {
        if (request.dueDate() != null) return request.dueDate();
        return computeDueDate(contract, request.billingYear(), request.billingMonth(), ownerId);
    }

    private LocalDate computeDueDate(RentalContract contract, int year, int month, Long ownerId) {
        int dueDay = (contract.getDueDay() != null && contract.getDueDay() > 0)
                ? contract.getDueDay() : ownerSettingService.getInvoiceDefaultDueDays(ownerId);
        int lastDay = YearMonth.of(year, month).lengthOfMonth();
        return LocalDate.of(year, month, Math.min(dueDay, lastDay));
    }

    private UtilityReading readingAtOrBefore(Long meterId, LocalDate date) {
        List<UtilityReading> readings = invoiceRepository.findReadingsForPeriod(meterId, date);
        return readings.isEmpty() ? null : readings.get(0);
    }

    private UtilityReading readingOnOrBefore(Long meterId, LocalDate date) {
        List<UtilityReading> readings = invoiceRepository.findReadingsForPeriod(meterId, date);
        return readings.isEmpty() ? null : readings.get(0);
    }

    private void verifyOwnerOrManagerRole() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            throw new AccessDeniedException("Only owners and managers can manage invoices");
        }
    }

    /**
     * Resolves the house scope for invoice generation. When a specific house is
     * requested it is validated against the current user's access; otherwise all
     * accessible houses are used (an owner/manager without houses gets no scope).
     */
    private List<Long> resolveEligibleHouseScope(Long houseId) {
        if (houseId != null) {
            houseAccessService.requireHouseAccess(houseId);
            return List.of(houseId);
        }
        List<Long> accessibleHouseIds = houseAccessService.getAccessibleHouseIds();
        return accessibleHouseIds != null ? accessibleHouseIds : List.of();
    }

    private YearMonth resolveBillingPeriod(Integer billingYear, Integer billingMonth) {
        YearMonth now = YearMonth.now();
        int year = billingYear != null ? billingYear : now.getYear();
        int month = billingMonth != null ? billingMonth : now.getMonthValue();
        if (year < 2000) {
            throw new InvalidInvoiceOperationException("Billing year must be at least 2000");
        }
        if (month < 1 || month > 12) {
            throw new InvalidInvoiceOperationException("Billing month must be between 1 and 12");
        }
        return YearMonth.of(year, month);
    }

    private User getCurrentOwner() {
        Object principal = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        org.springframework.security.core.userdetails.UserDetails userDetails =
                (org.springframework.security.core.userdetails.UserDetails) principal;
        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
