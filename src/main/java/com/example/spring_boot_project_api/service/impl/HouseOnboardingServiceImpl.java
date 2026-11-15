package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.HouseImportRequest;
import com.example.spring_boot_project_api.dto.request.HouseImportRowRequest;
import com.example.spring_boot_project_api.dto.request.HouseManualSetupRequest;
import com.example.spring_boot_project_api.dto.request.HouseManualSetupPreviewRequest;
import com.example.spring_boot_project_api.dto.response.HouseImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.HouseImportPreviewRowResponse;
import com.example.spring_boot_project_api.dto.response.HouseImportResult;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.HouseImportValidationException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.mapper.TenantMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.HouseOnboardingService;
import com.example.spring_boot_project_api.util.ImportFileParser;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HouseOnboardingServiceImpl implements HouseOnboardingService {

    private static final int MAX_ROWS = 500;
    private static final int MAX_CELL_LENGTH = 50;

    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.BASIC_ISO_DATE,
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
    };

    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final TenantRepository tenantRepository;
    private final RentalContractRepository rentalContractRepository;
    private final ContractTenantRepository contractTenantRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final TenantMapper tenantMapper;
    private final SecurityUtils securityUtils;

    // ------------------------------------------------------------------
    // Import flow
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public HouseImportPreviewResponse previewImport(Long houseId, String fileName, byte[] content) {
        List<HouseImportRowRequest> rows = parseImport(houseId, content);
        List<HouseImportPreviewRowResponse> previews = validateRows(houseId, rows);
        return HouseImportPreviewResponse.of(houseId, fileName, previews);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseImportRowRequest> parseImport(Long houseId, byte[] content) {
        requireManage(houseId);
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        List<List<String>> records;
        try {
            records = ImportFileParser.parseFileToRecords(content);
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Could not read the uploaded file. Use a valid .csv or .xlsx file.");
        }
        if (records.isEmpty()) {
            throw new IllegalArgumentException("No data rows found in the uploaded file");
        }
        List<HouseImportRowRequest> rows = new ArrayList<>();
        for (int i = 1; i < records.size() && rows.size() < MAX_ROWS; i++) {
            rows.add(toRowFromStrings(records.get(i)));
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("No data rows found in the uploaded file");
        }
        return rows;
    }

    @Override
    @Transactional
    public HouseImportResult importExisting(Long houseId, HouseImportRequest request) {
        if (request == null || request.rows() == null || request.rows().isEmpty()) {
            throw new IllegalArgumentException("At least one import row is required");
        }
        if (request.rows().size() > MAX_ROWS) {
            throw new IllegalArgumentException("Too many rows (max " + MAX_ROWS + ")");
        }

        requireManage(houseId);
        List<HouseImportPreviewRowResponse> validations = validateRows(houseId, request.rows());
        Map<Integer, List<String>> invalid = new HashMap<>();
        for (HouseImportPreviewRowResponse v : validations) {
            if (!v.valid()) {
                invalid.put(v.rowNumber(), v.errors());
            }
        }
        if (!invalid.isEmpty()) {
            throw new HouseImportValidationException(
                    "Cannot import: " + invalid.size() + " row(s) failed validation.",
                    invalid);
        }

        House house = requireHouse(houseId);
        User owner = house.getOwner();

        int roomsCreated = 0;
        int tenantsCreated = 0;
        int tenantsReused = 0;
        int contractsCreated = 0;
        int contractTenantsCreated = 0;
        List<String> messages = new ArrayList<>();
        Map<Long, Boolean> tenantNewlyCreated = new HashMap<>();

        for (int i = 0; i < request.rows().size(); i++) {
            HouseImportRowRequest row = request.rows().get(i);
            int rowNumber = i + 2;
            Room room = ensureRoom(house, owner, row.roomNo(), row.rent(), row.deposit(),
                    rowNumber, messages);
            if (room.getStatus() == RoomStatus.AVAILABLE) {
                roomsCreated++;
            }

            Tenant tenant = ensureTenant(house, owner, row, rowNumber, messages,
                    tenantNewlyCreated);
            if (Boolean.TRUE.equals(tenantNewlyCreated.get(tenant.getId()))) {
                tenantsCreated++;
            } else {
                tenantsReused++;
            }

            LocalDate moveIn = parseDate(row.moveInDate());
            RentalContract contract = RentalContract.builder()
                    .ownerId(owner.getId())
                    .contractNo(generateContractNo(owner.getId()))
                    .roomId(room.getId())
                    .startDate(moveIn)
                    .endDate(null)
                    .monthlyRent(parseMoney(row.rent()))
                    .securityDeposit(parseMoneyOrDefault(row.deposit(), BigDecimal.ZERO))
                    .dueDay(row.dueDay() != null ? row.dueDay() : 5)
                    .status(parseStatus(row.contractStatus()))
                    .notes(row.notes())
                    .build();
            rentalContractRepository.save(contract);
            contractsCreated++;

            ContractTenant ct = ContractTenant.builder()
                    .contractId(contract.getId())
                    .tenantId(tenant.getId())
                    .isPrimary(true)
                    .joinedAt(moveIn)
                    .build();
            contractTenantRepository.save(ct);
            contractTenantsCreated++;

            if (room.getStatus() == RoomStatus.AVAILABLE) {
                room.setStatus(RoomStatus.OCCUPIED);
                roomRepository.save(room);
            }
            if (tenant.getStatus() != TenantStatus.RENTING) {
                tenant.setStatus(TenantStatus.RENTING);
                tenantRepository.save(tenant);
            }
        }

        return HouseImportResult.of(roomsCreated, tenantsCreated, tenantsReused, contractsCreated,
                contractTenantsCreated, List.copyOf(messages));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponse> getHouseOwnerTenants(Long houseId) {
        requireManage(houseId);
        House house = requireHouse(houseId);
        User owner = house.getOwner();
        return tenantRepository.findAllByOwnerIdOrderByCreatedAtDesc(owner.getId())
                .stream()
                .map(tenantMapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Manual existing house setup
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public HouseImportPreviewResponse previewManualSetup(Long houseId,
            HouseManualSetupPreviewRequest request) {
        if (request == null || request.rows() == null || request.rows().isEmpty()) {
            throw new IllegalArgumentException("At least one entry is required");
        }
        requireManage(houseId);
        House house = requireHouse(houseId);
        User owner = house.getOwner();

        Set<String> seenRooms = new java.util.HashSet<>();
        Set<String> seenPhones = new java.util.HashSet<>();
        List<HouseImportPreviewRowResponse> previews = new ArrayList<>();

        Set<String> existingRooms = new java.util.HashSet<>(
                roomRepository.findByHouseId(houseId).stream()
                        .map(Room::getRoomNumber)
                        .toList());
        Map<String, Optional<Tenant>> tenantByPhone = new HashMap<>();

        for (int i = 0; i < request.rows().size(); i++) {
            HouseManualSetupPreviewRequest.HouseManualSetupPreviewRowRequest row = request.rows()
                    .get(i);
            int rowNumber = i + 1;
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            String roomNo = row.roomNumber();
            if (roomNo == null || roomNo.isBlank()) {
                errors.add("Room number is required.");
            } else if (existingRooms.contains(roomNo) || !seenRooms.add(roomNo)) {
                errors.add("Room " + roomNo + " already exists.");
            }

            String tenantFirstName = row.tenantFirstName();
            if (tenantFirstName == null || tenantFirstName.isBlank()) {
                errors.add("Tenant first name is required.");
            }

            String phone = row.tenantPhone();
            if (phone != null && !phone.isBlank()) {
                if (seenPhones.contains(phone)) {
                    warnings.add("Duplicate phone number within this entry; existing tenant will not be matched.");
                } else {
                    seenPhones.add(phone);
                    Optional<Tenant> existingTenant = tenantByPhone
                            .computeIfAbsent(phone,
                                    p -> tenantRepository.findByOwnerIdAndPhone(owner.getId(), p));
                    if (existingTenant.isPresent()) {
                        warnings.add("Existing tenant matched by phone; it will be reused rather than duplicated.");
                    }
                }
            }

            if (row.moveInDate() == null) {
                errors.add("Move-in date is required.");
            }

            if (row.monthlyRent() == null) {
                errors.add("Monthly rent is required.");
            } else if (row.monthlyRent().signum() < 0) {
                errors.add("Monthly rent cannot be negative.");
            }

            if (row.securityDeposit() != null && row.securityDeposit().signum() < 0) {
                errors.add("Security deposit cannot be negative.");
            }

            RentalContractStatus status = row.contractStatus() != null
                    ? row.contractStatus()
                    : RentalContractStatus.ACTIVE;

            if (row.dueDay() != null && (row.dueDay() < 1 || row.dueDay() > 31)) {
                errors.add("Due day must be between 1 and 31.");
            }

            previews.add(HouseImportPreviewRowResponse.of(rowNumber, roomNo,
                    (row.tenantFirstName() != null ? row.tenantFirstName() : "")
                            + (row.tenantLastName() != null ? " " + row.tenantLastName() : ""),
                    phone, row.tenantEmail(),
                    row.moveInDate() != null ? row.moveInDate().toString() : null,
                    row.monthlyRent() != null ? row.monthlyRent().toPlainString() : null,
                    row.securityDeposit() != null ? row.securityDeposit().toPlainString() : null,
                    status, row.dueDay(), errors, warnings));
        }

        return HouseImportPreviewResponse.of(houseId, "manual-entry", previews);
    }

    @Override
    @Transactional
    public HouseImportResult manualSetup(Long houseId, HouseManualSetupRequest request) {
        if (request == null || request.rows() == null || request.rows().isEmpty()) {
            throw new IllegalArgumentException("At least one entry is required");
        }
        if (request.rows().size() > MAX_ROWS) {
            throw new IllegalArgumentException("Too many rows (max " + MAX_ROWS + ")");
        }

        requireManage(houseId);

        // Build preview rows for validation
        HouseManualSetupPreviewRequest previewReq = new HouseManualSetupPreviewRequest(
                request.rows().stream()
                        .map(r -> new HouseManualSetupPreviewRequest.HouseManualSetupPreviewRowRequest(
                                r.roomNumber(), r.tenantFirstName(), r.tenantLastName(),
                                r.tenantPhone(), r.tenantEmail(), r.moveInDate(),
                                r.monthlyRent(), r.securityDeposit(), r.dueDay(),
                                r.contractStatus(), r.notes()))
                        .toList());
        HouseImportPreviewResponse preview = previewManualSetup(houseId, previewReq);
        Map<Integer, List<String>> invalid = new HashMap<>();
        for (HouseImportPreviewRowResponse v : preview.rows()) {
            if (!v.valid()) {
                invalid.put(v.rowNumber(), v.errors());
            }
        }
        if (!invalid.isEmpty()) {
            throw new HouseImportValidationException(
                    "Cannot save: " + invalid.size() + " row(s) failed validation.", invalid);
        }

        House house = requireHouse(houseId);
        User owner = house.getOwner();

        int roomsCreated = 0;
        int tenantsCreated = 0;
        int tenantsReused = 0;
        int contractsCreated = 0;
        int contractTenantsCreated = 0;
        List<String> messages = new ArrayList<>();
        Map<Long, Boolean> tenantNewlyCreated = new HashMap<>();

        for (int i = 0; i < request.rows().size(); i++) {
            HouseManualSetupRequest.HouseManualSetupRowRequest row = request.rows().get(i);
            int rowNumber = i + 1;

            Room room = ensureRoom(house, owner, row.roomNumber(),
                    row.monthlyRent().toPlainString(),
                    row.securityDeposit() != null ? row.securityDeposit().toPlainString() : "0",
                    rowNumber, messages);
            if (room.getStatus() == RoomStatus.AVAILABLE) {
                roomsCreated++;
            }

            Tenant tenant = ensureTenantManual(house, owner, row, rowNumber, messages,
                    tenantNewlyCreated);
            if (Boolean.TRUE.equals(tenantNewlyCreated.get(tenant.getId()))) {
                tenantsCreated++;
            } else {
                tenantsReused++;
            }

            RentalContract contract = RentalContract.builder()
                    .ownerId(owner.getId())
                    .contractNo(generateContractNo(owner.getId()))
                    .roomId(room.getId())
                    .startDate(row.moveInDate())
                    .endDate(null)
                    .monthlyRent(row.monthlyRent())
                    .securityDeposit(
                            row.securityDeposit() != null ? row.securityDeposit() : BigDecimal.ZERO)
                    .dueDay(row.dueDay() != null ? row.dueDay() : 5)
                    .status(row.contractStatus() != null ? row.contractStatus()
                            : RentalContractStatus.ACTIVE)
                    .notes(row.notes())
                    .build();
            rentalContractRepository.save(contract);
            contractsCreated++;

            ContractTenant ct = ContractTenant.builder()
                    .contractId(contract.getId())
                    .tenantId(tenant.getId())
                    .isPrimary(true)
                    .joinedAt(row.moveInDate())
                    .build();
            contractTenantRepository.save(ct);
            contractTenantsCreated++;

            if (room.getStatus() == RoomStatus.AVAILABLE) {
                room.setStatus(RoomStatus.OCCUPIED);
                roomRepository.save(room);
            }
            if (tenant.getStatus() != TenantStatus.RENTING) {
                tenant.setStatus(TenantStatus.RENTING);
                tenantRepository.save(tenant);
            }
        }

        return HouseImportResult.of(roomsCreated, tenantsCreated, tenantsReused, contractsCreated,
                contractTenantsCreated, List.copyOf(messages));
    }

    private Tenant ensureTenantManual(House house, User owner,
            HouseManualSetupRequest.HouseManualSetupRowRequest row, int rowNumber,
            List<String> messages, Map<Long, Boolean> tenantNewlyCreated) {
        if (row.tenantPhone() != null && !row.tenantPhone().isBlank()) {
            Optional<Tenant> existing = tenantRepository.findByOwnerIdAndPhone(owner.getId(),
                    row.tenantPhone());
            if (existing.isPresent()) {
                tenantNewlyCreated.put(existing.get().getId(), Boolean.FALSE);
                messages.add("Row " + rowNumber + ": existing tenant reused for "
                        + row.tenantFirstName() + ".");
                return existing.get();
            }
        }
        String fullName = row.tenantFirstName()
                + (row.tenantLastName() != null ? " " + row.tenantLastName() : "");
        Tenant tenant = Tenant.builder()
                .owner(owner)
                .house(house)
                .tenantCode(generateTenantCode(owner.getId()))
                .firstName(row.tenantFirstName())
                .lastName(row.tenantLastName() != null ? row.tenantLastName() : "")
                .phone(row.tenantPhone())
                .email(row.tenantEmail())
                .status(TenantStatus.ACTIVE)
                .build();
        Tenant saved = tenantRepository.save(tenant);
        tenantNewlyCreated.put(saved.getId(), Boolean.TRUE);
        messages.add("Row " + rowNumber + ": tenant " + fullName + " created.");
        return saved;
    }

    // ------------------------------------------------------------------
    // Import row parsing (CSV/Excel)
    // ------------------------------------------------------------------

    private HouseImportRowRequest toRowFromStrings(List<String> fields) {
        String roomNo = ImportFileParser.stringField(fields, 0, MAX_CELL_LENGTH);
        String tenantName = ImportFileParser.stringField(fields, 1, MAX_CELL_LENGTH);
        String phone = ImportFileParser.stringField(fields, 2, MAX_CELL_LENGTH);
        String email = ImportFileParser.stringField(fields, 3, MAX_CELL_LENGTH);
        String moveInDate = ImportFileParser.stringField(fields, 4, MAX_CELL_LENGTH);
        String rent = ImportFileParser.stringField(fields, 5, 20);
        String deposit = ImportFileParser.stringField(fields, 6, 20);
        String contractStatus = ImportFileParser.stringField(fields, 7, MAX_CELL_LENGTH);
        Integer dueDay = ImportFileParser.integerField(fields, 8);
        String notes = ImportFileParser.stringField(fields, 9, 255);
        return HouseImportRowRequest.of(roomNo, tenantName, phone, email, moveInDate, rent,
                deposit, contractStatus, dueDay, notes);
    }

    // ------------------------------------------------------------------
    // Validation (shared by preview and confirm)
    // ------------------------------------------------------------------

    private List<HouseImportPreviewRowResponse> validateRows(Long houseId, List<HouseImportRowRequest> rows) {
        requireManage(houseId);
        House house = requireHouse(houseId);
        User owner = house.getOwner();

        Set<String> seenRooms = new java.util.HashSet<>();
        Set<String> seenPhones = new java.util.HashSet<>();
        List<HouseImportPreviewRowResponse> previews = new ArrayList<>();

        // Existing room numbers / contract numbers for this house, pre-loaded once.
        Set<String> existingRooms = new java.util.HashSet<>(
                roomRepository.findByHouseId(houseId).stream()
                        .map(Room::getRoomNumber)
                        .toList());
        Map<String, Optional<Tenant>> tenantByPhone = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            HouseImportRowRequest row = rows.get(i);
            int rowNumber = i + 2;
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            String roomNo = row.roomNo();
            if (roomNo == null || roomNo.isBlank()) {
                errors.add("Room number is required.");
            } else if (existingRooms.contains(roomNo) || !seenRooms.add(roomNo)) {
                errors.add("Room " + roomNo + " already exists.");
            }

            String tenantName = row.tenantName();
            if (tenantName == null || tenantName.isBlank()) {
                errors.add("Tenant name is required.");
            }

            String phone = row.phone();
            if (phone == null || phone.isBlank()) {
                errors.add("Phone is required.");
            }
            if (phone != null) {
                if (seenPhones.contains(phone)) {
                    warnings.add("Duplicate phone number within this import; existing tenant will not be matched.");
                } else {
                    seenPhones.add(phone);
                    Optional<Tenant> existingTenant = tenantByPhone
                            .computeIfAbsent(phone, p -> tenantRepository.findByOwnerIdAndPhone(owner.getId(), p));
                    if (existingTenant.isPresent()) {
                        warnings.add("Existing tenant matched by phone; it will be reused rather than duplicated.");
                    }
                }
            }

            if (row.moveInDate() == null || row.moveInDate().isBlank()) {
                errors.add("Move-in date is required.");
            } else {
                LocalDate parsed = parseDateOrNull(row.moveInDate());
                if (parsed == null) {
                    errors.add("Invalid move-in date.");
                }
            }

            BigDecimal rent = parseMoneyOrNull(row.rent());
            if (row.rent() == null || row.rent().isBlank()) {
                errors.add("Monthly rent is required.");
            } else if (rent == null) {
                errors.add("Monthly rent is invalid.");
            } else if (rent.signum() < 0) {
                errors.add("Monthly rent cannot be negative.");
            }

            BigDecimal deposit = parseMoneyOrNull(row.deposit());
            if (row.deposit() != null && !row.deposit().isBlank() && deposit == null) {
                errors.add("Deposit is invalid.");
            } else if (deposit != null && deposit.signum() < 0) {
                errors.add("Deposit cannot be negative.");
            }

            RentalContractStatus status = null;
            if (row.contractStatus() == null || row.contractStatus().isBlank()) {
                status = RentalContractStatus.ACTIVE;
            } else {
                status = parseStatusOrNull(row.contractStatus());
                if (status == null) {
                    errors.add("Invalid contract status. Use ACTIVE, DRAFT, TERMINATED, EXPIRED or CANCELLED.");
                }
            }

            if (row.dueDay() != null && (row.dueDay() < 1 || row.dueDay() > 31)) {
                errors.add("Due day must be between 1 and 31.");
            }

            previews.add(HouseImportPreviewRowResponse.of(rowNumber, roomNo, tenantName, phone,
                    row.email(), row.moveInDate(), row.rent(), row.deposit(), status, row.dueDay(),
                    errors, warnings));
        }
        return previews;
    }

    // ------------------------------------------------------------------
    // Import execution helpers
    // ------------------------------------------------------------------

    private Room ensureRoom(House house, User owner, String roomNo, String rent, String deposit,
            int rowNumber, List<String> messages) {
        Optional<Room> existing = roomRepository.findByHouseIdAndRoomNumber(house.getId(), roomNo);
        if (existing.isPresent()) {
            return existing.get();
        }
        Long roomTypeId = resolveRoomTypeId(owner.getId());
        Room room = new Room();
        room.setOwnerId(owner.getId());
        room.setHouseId(house.getId());
        room.setRoomTypeId(roomTypeId);
        room.setRoomNumber(roomNo);
        room.setName(roomNo);
        room.setMonthlyRent(parseMoneyOrDefault(rent, BigDecimal.ZERO));
        room.setSecurityDeposit(parseMoneyOrDefault(deposit, BigDecimal.ZERO));
        room.setCapacity(1);
        room.setStatus(RoomStatus.AVAILABLE);
        Room saved = roomRepository.save(room);
        messages.add("Row " + rowNumber + ": room " + roomNo + " created.");
        return saved;
    }

    private Long resolveRoomTypeId(Long ownerId) {
        List<RoomType> types = roomTypeRepository.findByOwnerId(ownerId);
        if (types != null && !types.isEmpty()) {
            return types.get(0).getId();
        }
        RoomType created = new RoomType();
        created.setOwnerId(ownerId);
        created.setName("Default");
        created.setCode("DEFAULT" + ownerId);
        created.setCapacity(1);
        return roomTypeRepository.save(created).getId();
    }

    private Tenant ensureTenant(House house, User owner, HouseImportRowRequest row, int rowNumber,
            List<String> messages, Map<Long, Boolean> tenantNewlyCreated) {
        Optional<Tenant> existing = tenantRepository.findByOwnerIdAndPhone(owner.getId(), row.phone());
        if (existing.isPresent()) {
            tenantNewlyCreated.put(existing.get().getId(), Boolean.FALSE);
            messages.add("Row " + rowNumber + ": existing tenant reused for " + row.tenantName() + ".");
            return existing.get();
        }
        Tenant tenant = Tenant.builder()
                .owner(owner)
                .house(house)
                .tenantCode(generateTenantCode(owner.getId()))
                .firstName(firstName(row.tenantName()))
                .lastName(lastName(row.tenantName()))
                .phone(row.phone())
                .email(row.email())
                .status(TenantStatus.ACTIVE)
                .build();
        Tenant saved = tenantRepository.save(tenant);
        tenantNewlyCreated.put(saved.getId(), Boolean.TRUE);
        messages.add("Row " + rowNumber + ": tenant " + row.tenantName() + " created.");
        return saved;
    }

    private String firstName(String fullName) {
        if (fullName == null) {
            return "";
        }
        int sp = fullName.indexOf(' ');
        return sp < 0 ? fullName.trim() : fullName.substring(0, sp).trim();
    }

    private String lastName(String fullName) {
        if (fullName == null) {
            return "";
        }
        int sp = fullName.indexOf(' ');
        return sp < 0 ? "" : fullName.substring(sp + 1).trim();
    }

    private String generateContractNo(Long ownerId) {
        String base = "EX-" + ownerId + "-";
        long count = rentalContractRepository.findByOwnerId(ownerId).stream()
                .filter(c -> c.getContractNo() != null && c.getContractNo().startsWith(base))
                .count();
        long seq = count + 1;
        String candidate;
        do {
            candidate = base + String.format("%03d", seq);
            seq++;
        } while (rentalContractRepository.findByOwnerIdAndContractNo(ownerId, candidate).isPresent());
        return candidate;
    }

    private String generateTenantCode(Long ownerId) {
        String prefix = "T" + ownerId + "-";
        long count = tenantRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .filter(t -> t.getTenantCode() != null && t.getTenantCode().startsWith(prefix))
                .count();
        long seq = count + 1;
        String candidate;
        do {
            candidate = prefix + String.format("%04d", seq);
            seq++;
        } while (tenantRepository.existsByOwnerIdAndTenantCode(ownerId, candidate));
        return candidate;
    }

    // ------------------------------------------------------------------
    // Parsing helpers
    // ------------------------------------------------------------------

    private LocalDate parseDate(String value) {
        LocalDate parsed = parseDateOrNull(value);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid move-in date: " + value);
        }
        return parsed;
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(trimmed, fmt);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return null;
    }

    private BigDecimal parseMoney(String value) {
        BigDecimal parsed = parseMoneyOrNull(value);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid amount: " + value);
        }
        return parsed;
    }

    private BigDecimal parseMoneyOrDefault(String value, BigDecimal fallback) {
        BigDecimal parsed = parseMoneyOrNull(value);
        return parsed != null ? parsed : fallback;
    }

    private BigDecimal parseMoneyOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim().replace(",", "").replace("$", "");
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private RentalContractStatus parseStatus(String value) {
        RentalContractStatus status = parseStatusOrNull(value);
        return status != null ? status : RentalContractStatus.ACTIVE;
    }

    private RentalContractStatus parseStatusOrNull(String value) {
        if (value == null || value.isBlank()) {
            return RentalContractStatus.ACTIVE;
        }
        try {
            return RentalContractStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Authorization
    // ------------------------------------------------------------------

    private House requireHouse(Long houseId) {
        return houseRepository.findById(houseId)
                .orElseThrow(() -> HouseNotFoundException.forId(houseId));
    }

    private void requireManage(Long houseId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        House house = requireHouse(houseId);
        if (house.getOwner().getId().equals(currentUserId)) {
            return;
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)
                && userHouseAccessRepository.existsByUserIdAndHouseId(currentUserId, houseId)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to access this resource");
    }
}