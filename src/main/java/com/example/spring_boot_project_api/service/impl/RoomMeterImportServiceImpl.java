package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateUtilityMeterRequest;
import com.example.spring_boot_project_api.dto.request.RoomMeterImportRequest;
import com.example.spring_boot_project_api.dto.request.RoomMeterImportRow;
import com.example.spring_boot_project_api.dto.request.RoomRepuest;
import com.example.spring_boot_project_api.dto.response.RoomMeterImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.RoomMeterImportPreviewRowResponse;
import com.example.spring_boot_project_api.dto.response.RoomMeterImportResult;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.enums.UtilityBillingMethod;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.exception.RoomMeterImportValidationException;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.RoomUtility;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.RoomUtilityRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.RoomMeterImportService;
import com.example.spring_boot_project_api.service.RoomService;
import com.example.spring_boot_project_api.service.UtilityMeterService;
import com.example.spring_boot_project_api.util.ImportFileParser;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * CSV/Excel import that creates rooms and, optionally, utility meters attached to the created
 * rooms. The house, room type and optional meter install date are supplied once by the caller
 * (selected in the UI) and apply to every row; the CSV never contains a house, room type or
 * install date. Every row is validated (and authorized) individually, then all valid rows are
 * written in one transaction.
 *
 * <p>Columns: Room Number, Room Name, Floor, Monthly Rent, Deposit, Status, Description, then
 * the optional meter groups Electricity Meter / Electricity Status and Water Meter / Water
 * Status. Extra trailing columns are ignored.
 */
@Service
@RequiredArgsConstructor
public class RoomMeterImportServiceImpl implements RoomMeterImportService {

    private static final int MAX_ROWS = 500;
    private static final int MAX_ROOM_NUMBER = 50;
    private static final int MAX_METER_NUMBER = 100;

    private static final List<String> ELECTRICITY_KEYWORDS =
            List.of("elec", "electric", "electricity", "ភ្លើង");
    private static final List<String> WATER_KEYWORDS =
            List.of("wtr", "water", "ទឹក");

    private final RoomService roomService;
    private final HouseRepository houseRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final UtilityMeterRepository meterRepository;
    private final RoomUtilityRepository roomUtilityRepository;
    private final UtilityMeterService utilityMeterService;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public RoomMeterImportPreviewResponse previewImport(Long houseId, Long roomTypeId, String installDate,
            String fileName, byte[] content) {
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
        List<RoomMeterImportRow> rows = rowsFromFile(records);
        return RoomMeterImportPreviewResponse.of(fileName,
                validateRows(rows, houseId, roomTypeId, parseInstallDate(installDate)));
    }

    @Override
    @Transactional
    public RoomMeterImportResult importRoomsWithMeters(RoomMeterImportRequest request) {
        if (request == null || request.rows() == null || request.rows().isEmpty()) {
            throw new IllegalArgumentException("At least one import row is required");
        }
        if (request.rows().size() > MAX_ROWS) {
            throw new IllegalArgumentException("Too many rows (max " + MAX_ROWS + ")");
        }

        List<RoomMeterImportPreviewRowResponse> validations = validateRows(request.rows(),
                request.houseId(), request.roomTypeId(), request.installDate());
        Map<Integer, List<String>> invalid = new HashMap<>();
        for (RoomMeterImportPreviewRowResponse v : validations) {
            if (!v.valid()) {
                invalid.put(v.rowNumber(), v.errors());
            }
        }
        if (!invalid.isEmpty()) {
            throw new RoomMeterImportValidationException(
                    "Some rows are invalid. Nothing was imported.", invalid);
        }

        Long currentUserId = securityUtils.getCurrentUserId();
        House house = resolveHouseById(request.houseId(), currentUserId);
        Long ownerId = house.getOwner().getId();
        RoomType roomType = resolveRoomTypeById(request.roomTypeId(), ownerId);
        LocalDate installedAt = request.installDate() != null ? request.installDate() : LocalDate.now();

        List<String> messages = new ArrayList<>();
        int roomsCreated = 0;
        int metersCreated = 0;
        for (int i = 0; i < request.rows().size(); i++) {
            RoomMeterImportRow row = request.rows().get(i);
            int rowNumber = i + 2;

            RoomResponse saved = roomService.createRoom(toCreateRequest(row, house.getId(), roomType.getId()));
            roomsCreated++;

            List<String> rowMessages = new ArrayList<>();
            UtilityType electricity = matchesMeteredUtilityType(ownerId, ELECTRICITY_KEYWORDS);
            UtilityType water = matchesMeteredUtilityType(ownerId, WATER_KEYWORDS);

            if (row.electricityMeter() != null && electricity != null) {
                if (roomUtilityRepository.findByRoomIdAndUtilityTypeId(
                        saved.getId(), electricity.getId()).isEmpty()) {
                    roomUtilityRepository.save(RoomUtility.builder()
                            .owner(house.getOwner())
                            .roomId(saved.getId())
                            .utilityTypeId(electricity.getId())
                            .status(UtilityTypeStatus.ACTIVE)
                            .isIncluded(Boolean.TRUE)
                            .build());
                }
                utilityMeterService.createMeter(toMeterRequest(house.getId(), saved.getId(), electricity.getId(),
                        row.electricityMeter(), installedAt, row.electricityStatus()));
                metersCreated++;
                rowMessages.add("electricity meter " + row.electricityMeter()
                        + " created (Electricity utility attached).");
            }
            if (row.waterMeter() != null && water != null) {
                if (roomUtilityRepository.findByRoomIdAndUtilityTypeId(
                        saved.getId(), water.getId()).isEmpty()) {
                    roomUtilityRepository.save(RoomUtility.builder()
                            .owner(house.getOwner())
                            .roomId(saved.getId())
                            .utilityTypeId(water.getId())
                            .status(UtilityTypeStatus.ACTIVE)
                            .isIncluded(Boolean.TRUE)
                            .build());
                }
                utilityMeterService.createMeter(toMeterRequest(house.getId(), saved.getId(), water.getId(),
                        row.waterMeter(), installedAt, row.waterStatus()));
                metersCreated++;
                rowMessages.add("water meter " + row.waterMeter()
                        + " created (Water utility attached).");
            }
            String suffix = rowMessages.isEmpty() ? " (no meters)."
                    : " with " + String.join(" ", rowMessages);
            messages.add("Row " + rowNumber + ": room " + saved.getRoomNumber() + " created" + suffix);
        }
        return RoomMeterImportResult.of(roomsCreated, metersCreated, messages);
    }

    // ------------------------------------------------------------------
    // Row mapping from parsed file
    // ------------------------------------------------------------------

    private List<RoomMeterImportRow> rowsFromFile(List<List<String>> records) {
        if (records.isEmpty()) {
            throw new IllegalArgumentException("No data rows found in the uploaded file");
        }
        List<RoomMeterImportRow> rows = new ArrayList<>();
        for (int i = 1; i < records.size() && rows.size() < MAX_ROWS; i++) {
            rows.add(toRow(records.get(i)));
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("No data rows found in the uploaded file");
        }
        return rows;
    }

    private RoomMeterImportRow toRow(List<String> record) {
        return RoomMeterImportRow.of(
                ImportFileParser.stringField(record, 0, MAX_ROOM_NUMBER),
                ImportFileParser.stringField(record, 1, 100),
                ImportFileParser.stringField(record, 2, 10),
                ImportFileParser.stringField(record, 3, 20),
                ImportFileParser.stringField(record, 4, 20),
                ImportFileParser.stringField(record, 5, 20),
                ImportFileParser.stringField(record, 6, 1000),
                ImportFileParser.stringField(record, 7, MAX_METER_NUMBER),
                ImportFileParser.stringField(record, 8, 20),
                ImportFileParser.stringField(record, 9, MAX_METER_NUMBER),
                ImportFileParser.stringField(record, 10, 20));
    }

    private RoomRepuest toCreateRequest(RoomMeterImportRow row, Long houseId, Long roomTypeId) {
        RoomRepuest req = new RoomRepuest();
        req.setHouseId(houseId);
        req.setRoomTypeId(roomTypeId);
        req.setRoomNumber(row.roomNumber());
        req.setName(row.name());
        req.setFloorNumber(parseInteger(row.floorNumber()));
        BigDecimal rent = parseMoney(row.monthlyRent());
        req.setMonthlyRent(rent != null ? rent : BigDecimal.ZERO);
        BigDecimal deposit = parseMoney(row.securityDeposit());
        req.setSecurityDeposit(deposit != null ? deposit : BigDecimal.ZERO);
        req.setStatus(parseStatus(row.status()));
        req.setDescription(row.description());
        return req;
    }

    private CreateUtilityMeterRequest toMeterRequest(Long houseId, Long roomId, Long utilityTypeId,
            String meterNumber, LocalDate installedAt, String status) {
        return new CreateUtilityMeterRequest(
                houseId,
                roomId,
                utilityTypeId,
                meterNumber,
                installedAt,
                parseMeterStatus(status));
    }

    // ------------------------------------------------------------------
    // Validation (shared by preview and confirm)
    // ------------------------------------------------------------------

    private List<RoomMeterImportPreviewRowResponse> validateRows(List<RoomMeterImportRow> rows,
            Long houseId, Long roomTypeId, LocalDate installDate) {
        Long currentUserId = securityUtils.getCurrentUserId();

        House house = null;
        Long ownerId = null;
        if (houseId != null) {
            house = resolveHouseById(houseId, currentUserId);
            if (house != null) {
                ownerId = house.getOwner().getId();
            }
        }

        RoomType roomType = null;
        if (ownerId != null) {
            roomType = resolveRoomTypeById(roomTypeId, ownerId);
        }

        String houseLabel = house != null ? house.getName()
                : (houseId != null ? String.valueOf(houseId) : null);
        String roomTypeLabel = roomType != null ? roomType.getName()
                : (roomTypeId != null ? String.valueOf(roomTypeId) : null);

        Map<Long, Set<String>> seenRoomsByHouse = new HashMap<>();
        Map<String, Integer> seenMeterNumbers = new HashMap<>();

        List<RoomMeterImportPreviewRowResponse> previews = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            RoomMeterImportRow row = rows.get(i);
            int rowNumber = i + 2;
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            int metersToCreate = 0;

            if (houseId == null) {
                errors.add("House is required.");
            } else if (house == null) {
                errors.add("House not found or you do not have access to it.");
            }

            if (ownerId != null) {
                if (roomTypeId == null) {
                    errors.add("Room Type is required.");
                } else if (roomType == null) {
                    errors.add("Room Type not found or invalid for this House.");
                }
            }

            if (row.roomNumber() == null || row.roomNumber().isBlank()) {
                errors.add("RoomNumber is required.");
            } else if (house != null) {
                final Long resolvedHouseId = house.getId();
                Set<String> seen = seenRoomsByHouse.computeIfAbsent(
                        resolvedHouseId, k -> new HashSet<>(roomRepository.findByHouseId(resolvedHouseId).stream()
                                .map(r -> r.getRoomNumber())
                                .toList()));
                if (!seen.add(row.roomNumber())) {
                    errors.add("Room number already exists in this house: " + row.roomNumber());
                }
            }

            if (row.monthlyRent() == null || row.monthlyRent().isBlank()) {
                errors.add("MonthlyRent is required.");
            } else if (parseMoney(row.monthlyRent()) == null) {
                errors.add("MonthlyRent is invalid.");
            } else if (parseMoney(row.monthlyRent()).signum() < 0) {
                errors.add("MonthlyRent cannot be negative.");
            }

            if (row.securityDeposit() != null && !row.securityDeposit().isBlank()) {
                BigDecimal deposit = parseMoney(row.securityDeposit());
                if (deposit == null) {
                    errors.add("SecurityDeposit is invalid.");
                } else if (deposit.signum() < 0) {
                    errors.add("SecurityDeposit cannot be negative.");
                }
            }

            if (row.floorNumber() != null && !row.floorNumber().isBlank()
                    && parseInteger(row.floorNumber()) == null) {
                errors.add("FloorNumber is invalid.");
            }

            if (row.status() != null && !row.status().isBlank()
                    && parseStatus(row.status()) == null) {
                errors.add("Invalid status. Use AVAILABLE, OCCUPIED, MAINTENANCE or RESERVED.");
            }

            // --- Meters ---
            UtilityType electricityType = ownerId != null
                    ? matchesMeteredUtilityType(ownerId, ELECTRICITY_KEYWORDS) : null;
            UtilityType waterType = ownerId != null
                    ? matchesMeteredUtilityType(ownerId, WATER_KEYWORDS) : null;
            metersToCreate += validateMeterField(row.electricityMeter(), row.electricityStatus(),
                    ownerId, electricityType, "Electricity", rowNumber, seenMeterNumbers, errors, warnings);
            metersToCreate += validateMeterField(row.waterMeter(), row.waterStatus(),
                    ownerId, waterType, "Water", rowNumber, seenMeterNumbers, errors, warnings);

            previews.add(RoomMeterImportPreviewRowResponse.of(
                    rowNumber, houseLabel, roomTypeLabel, row.roomNumber(), row.name(),
                    row.floorNumber(), row.monthlyRent(), row.securityDeposit(), row.status(),
                    row.description(), row.electricityMeter(), row.waterMeter(),
                    metersToCreate, errors, warnings));
        }
        return previews;
    }

    /**
     * Validates one optional meter column group. Returns 1 if a meter will be created, 0 otherwise.
     *
     * <p>Uniqueness is scoped to OWNER + UTILITY TYPE + METER NUMBER: the same number is allowed
     * for a different owner or a different utility type, but is a duplicate for the exact same
     * owner + utility type combination.</p>
     */
    private int validateMeterField(String meterNumber, String meterStatus, Long ownerId,
            UtilityType utilityType, String kindLabel, int rowNumber,
            Map<String, Integer> seenMeterNumbers, List<String> errors, List<String> warnings) {
        if (meterNumber == null || meterNumber.isBlank()) {
            return 0;
        }
        List<String> meterErrors = new ArrayList<>();
        if (meterNumber.length() > MAX_METER_NUMBER) {
            meterErrors.add(kindLabel + " meter number must not exceed 100 characters.");
        }
        if (utilityType == null) {
            warnings.add(kindLabel + " meter skipped: no matching METERED utility type found for this owner.");
            return 0;
        }
        if (meterStatus != null && !meterStatus.isBlank() && parseMeterStatus(meterStatus) == null) {
            meterErrors.add(kindLabel + " meter status is invalid. Use ACTIVE, INACTIVE or REPLACED.");
        }
        String fileKey = ownerId + ":" + utilityType.getId() + ":" + meterNumber;
        if (ownerId != null
                && meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(
                        ownerId, utilityType.getId(), meterNumber)) {
            meterErrors.add("Meter number already exists: " + meterNumber);
        } else {
            Integer existingRow = seenMeterNumbers.get(fileKey);
            if (existingRow != null) {
                meterErrors.add("Duplicate meter number in file (row " + existingRow + "): " + meterNumber);
            } else {
                seenMeterNumbers.put(fileKey, rowNumber);
            }
        }
        errors.addAll(meterErrors);
        return meterErrors.isEmpty() ? 1 : 0;
    }

    // ------------------------------------------------------------------
    // Resolution helpers
    // ------------------------------------------------------------------

    private House resolveHouseById(Long houseId, Long currentUserId) {
        if (houseId == null) {
            return null;
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            House house = houseRepository.findById(houseId).orElse(null);
            if (house == null) {
                return null;
            }
            return userHouseAccessRepository.existsByUserIdAndHouseId(currentUserId, houseId)
                    ? house : null;
        }
        return houseRepository.findByIdAndOwnerId(houseId, currentUserId).orElse(null);
    }

    private RoomType resolveRoomTypeById(Long roomTypeId, Long ownerId) {
        if (roomTypeId == null) {
            return null;
        }
        RoomType roomType = roomTypeRepository.findById(roomTypeId).orElse(null);
        return roomType != null && ownerId.equals(roomType.getOwnerId()) ? roomType : null;
    }

    private UtilityType matchesMeteredUtilityType(Long ownerId, List<String> keywords) {
        if (ownerId == null) {
            return null;
        }
        return utilityTypeRepository.findByOwnerIdIn(List.of(ownerId)).stream()
                .filter(t -> t.getBillingMethod() == UtilityBillingMethod.METERED
                        && matchesKind(t, keywords))
                .findFirst().orElse(null);
    }

    private boolean matchesKind(UtilityType type, List<String> keywords) {
        String name = type.getName() == null ? "" : type.getName().toLowerCase(Locale.ROOT);
        String code = type.getCode() == null ? "" : type.getCode().toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (name.contains(keyword) || code.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Parsing helpers
    // ------------------------------------------------------------------

    private LocalDate parseInstallDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        LocalDate date = parseDate(value);
        if (date == null) {
            throw new IllegalArgumentException("Install date is invalid. Use yyyy-MM-dd.");
        }
        return date;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseMoney(String value) {
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

    private RoomStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return RoomStatus.AVAILABLE;
        }
        try {
            return RoomStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private UtilityMeterStatus parseMeterStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UtilityMeterStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}