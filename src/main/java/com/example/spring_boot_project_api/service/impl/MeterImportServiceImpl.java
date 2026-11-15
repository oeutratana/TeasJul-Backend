package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateUtilityMeterRequest;
import com.example.spring_boot_project_api.dto.request.MeterImportRequest;
import com.example.spring_boot_project_api.dto.request.MeterImportRow;
import com.example.spring_boot_project_api.dto.response.MeterImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.MeterImportPreviewRowResponse;
import com.example.spring_boot_project_api.dto.response.MeterImportResult;
import com.example.spring_boot_project_api.dto.response.UtilityMeterResponse;
import com.example.spring_boot_project_api.enums.UtilityBillingMethod;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.exception.MeterImportValidationException;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.UtilityMeter;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.MeterImportService;
import com.example.spring_boot_project_api.service.UtilityMeterService;
import com.example.spring_boot_project_api.util.ImportFileParser;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * CSV/Excel import for utility meters. Rows can reference different houses, so each row is
 * validated (and authorized) individually, then all valid rows are written in one transaction.
 */
@Service
@RequiredArgsConstructor
public class MeterImportServiceImpl implements MeterImportService {

    private static final int MAX_ROWS = 500;
    private static final int MAX_METER_NUMBER = 100;

    private final UtilityMeterService utilityMeterService;
    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final UtilityMeterRepository meterRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public MeterImportPreviewResponse previewImport(String fileName, byte[] content) {
        return previewImport(null, fileName, content);
    }

    @Override
    @Transactional(readOnly = true)
    public MeterImportPreviewResponse previewImport(Long houseId, String fileName, byte[] content) {
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
        List<MeterImportRow> rows = rowsFromRecords(records, houseId);
        return MeterImportPreviewResponse.of(fileName, validateRows(rows));
    }

    @Override
    @Transactional
    public MeterImportResult importMeters(MeterImportRequest request) {
        if (request == null || request.rows() == null || request.rows().isEmpty()) {
            throw new IllegalArgumentException("At least one import row is required");
        }
        if (request.rows().size() > MAX_ROWS) {
            throw new IllegalArgumentException("Too many rows (max " + MAX_ROWS + ")");
        }

        List<MeterImportRow> rowsToValidate = request.rows();
        if (request.houseId() != null) {
            String defaultHouseIdStr = String.valueOf(request.houseId());
            rowsToValidate = request.rows().stream()
                    .map(r -> new MeterImportRow(
                            defaultHouseIdStr,
                            r.roomId(),
                            r.utilityTypeId(),
                            r.meterNumber(),
                            r.installedAt(),
                            r.status()
                    ))
                    .toList();
        }

        List<MeterImportPreviewRowResponse> validations = validateRows(rowsToValidate);
        Map<Integer, List<String>> invalid = new HashMap<>();
        for (MeterImportPreviewRowResponse v : validations) {
            if (!v.valid()) {
                invalid.put(v.rowNumber(), v.errors());
            }
        }
        if (!invalid.isEmpty()) {
            throw new MeterImportValidationException(
                    "Some rows are invalid. Nothing was imported.", invalid);
        }

        List<String> messages = new ArrayList<>();
        int created = 0;
        for (int i = 0; i < rowsToValidate.size(); i++) {
            MeterImportRow row = rowsToValidate.get(i);
            UtilityMeterResponse saved = utilityMeterService.createMeter(toCreateRequest(row));
            created++;
            messages.add("Row " + (i + 2) + ": meter " + saved.meterNumber() + " created.");
        }
        return MeterImportResult.of(created, messages);
    }

    // ------------------------------------------------------------------
    // Row mapping from parsed file
    // ------------------------------------------------------------------

    private List<MeterImportRow> rowsFromRecords(List<List<String>> records, Long defaultHouseId) {
        if (records.isEmpty()) {
            throw new IllegalArgumentException("No data rows found in the uploaded file");
        }
        List<String> header = records.get(0);
        boolean hasHouseIdHeader = header != null && !header.isEmpty()
                && header.get(0).trim().equalsIgnoreCase("houseid");

        List<MeterImportRow> rows = new ArrayList<>();
        for (int i = 1; i < records.size() && rows.size() < MAX_ROWS; i++) {
            rows.add(toRow(records.get(i), hasHouseIdHeader, defaultHouseId));
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("No data rows found in the uploaded file");
        }
        return rows;
    }

    private MeterImportRow toRow(List<String> record, boolean hasHouseIdHeader, Long defaultHouseId) {
        String houseIdStr;
        int offset;
        if (hasHouseIdHeader) {
            houseIdStr = defaultHouseId != null ? String.valueOf(defaultHouseId)
                    : ImportFileParser.stringField(record, 0, 20);
            offset = 1;
        } else {
            houseIdStr = defaultHouseId != null ? String.valueOf(defaultHouseId) : null;
            offset = 0;
        }

        return MeterImportRow.of(
                houseIdStr,
                ImportFileParser.stringField(record, offset + 0, 20),
                ImportFileParser.stringField(record, offset + 1, 20),
                ImportFileParser.stringField(record, offset + 2, MAX_METER_NUMBER),
                ImportFileParser.stringField(record, offset + 3, 10),
                ImportFileParser.stringField(record, offset + 4, 20));
    }

    private CreateUtilityMeterRequest toCreateRequest(MeterImportRow row) {
        return new CreateUtilityMeterRequest(
                parseId(row.houseId()),
                parseId(row.roomId()),
                parseId(row.utilityTypeId()),
                row.meterNumber(),
                parseDate(row.installedAt()),
                parseStatus(row.status()));
    }

    // ------------------------------------------------------------------
    // Validation (shared by preview and confirm)
    // ------------------------------------------------------------------

    private List<MeterImportPreviewRowResponse> validateRows(List<MeterImportRow> rows) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Map<Long, Long> ownerByHouse = new HashMap<>();
        Map<String, Integer> seenMeterNumbers = new HashMap<>();
        Map<String, Boolean> activeRoomTypeSeen = new HashMap<>();

        List<MeterImportPreviewRowResponse> previews = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            MeterImportRow row = rows.get(i);
            int rowNumber = i + 2;
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            String meterNumber = row.meterNumber();
            if (meterNumber == null || meterNumber.isBlank()) {
                errors.add("MeterNumber is required.");
            } else if (meterNumber.length() > MAX_METER_NUMBER) {
                errors.add("MeterNumber must not exceed 100 characters.");
            }

            Long houseId = parseId(row.houseId());
            Long ownerId = null;
            if (houseId == null) {
                errors.add("HouseId is required.");
            } else {
                ownerId = ownerByHouse.get(houseId);
                if (ownerId == null) {
                    ownerId = resolveOwnerId(houseId, currentUserId);
                    if (ownerId == null) {
                        errors.add("House not found or you do not have access to it.");
                    } else {
                        ownerByHouse.put(houseId, ownerId);
                    }
                }
            }

            Long utilityTypeId = parseId(row.utilityTypeId());
            if (utilityTypeId == null) {
                errors.add("UtilityTypeId is required.");
            } else if (ownerId != null) {
                UtilityType type = utilityTypeRepository.findById(utilityTypeId).orElse(null);
                if (type == null) {
                    errors.add("Utility type not found.");
                } else if (!ownerId.equals(type.getOwner().getId())) {
                    errors.add("Utility type does not belong to the house owner.");
                } else if (type.getBillingMethod() != UtilityBillingMethod.METERED) {
                    errors.add("A meter can only be created for a METERED utility type "
                            + "(fixed utilities do not need meters).");
                }
            }

            Long roomId = parseId(row.roomId());
            if (roomId != null && houseId != null && ownerId != null) {
                Room room = roomRepository.findById(roomId).orElse(null);
                if (room == null) {
                    errors.add("Room not found with id: " + roomId);
                } else {
                    if (!ownerId.equals(room.getOwnerId())) {
                        errors.add("Room " + roomId + " does not belong to this owner.");
                    } else if (!houseId.equals(room.getHouseId())) {
                        errors.add("Room " + roomId + " does not belong to house " + houseId + ".");
                    }
                }
            }

            if (meterNumber != null && !meterNumber.isBlank()
                    && meterNumber.length() <= MAX_METER_NUMBER) {
                String fileKey = ownerId + ":" + utilityTypeId + ":" + meterNumber;
                if (ownerId != null && utilityTypeId != null
                        && meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(
                                ownerId, utilityTypeId, meterNumber)) {
                    errors.add("Meter number already exists: " + meterNumber);
                } else {
                    Integer existingRow = seenMeterNumbers.get(fileKey);
                    if (existingRow != null) {
                        errors.add("Duplicate meter number in file (row " + existingRow + "): " + meterNumber);
                    } else {
                        seenMeterNumbers.put(fileKey, rowNumber);
                    }
                }
            }

            UtilityMeterStatus parsedStatus = parseStatus(row.status());
            boolean effectiveActive = parsedStatus == null || parsedStatus == UtilityMeterStatus.ACTIVE;
            if (roomId != null && utilityTypeId != null) {
                String key = roomId + ":" + utilityTypeId;
                if (!activeRoomTypeSeen.containsKey(key)) {
                    boolean existingActive = meterRepository.findByRoomIdAndUtilityTypeId(roomId, utilityTypeId)
                            .stream().anyMatch(m -> m.getStatus() == UtilityMeterStatus.ACTIVE);
                    activeRoomTypeSeen.put(key, existingActive);
                }
                boolean activeSeen = activeRoomTypeSeen.get(key);
                if (activeSeen && effectiveActive) {
                    errors.add("This room already has an active meter of this utility type.");
                }
                activeRoomTypeSeen.put(key, activeSeen || effectiveActive);
            }

            if (row.installedAt() != null && !row.installedAt().isBlank()) {
                if (parseDate(row.installedAt()) == null) {
                    errors.add("InstalledAt is invalid. Use yyyy-MM-dd.");
                }
            }

            if (row.status() != null && !row.status().isBlank() && parsedStatus == null) {
                errors.add("Invalid status. Use ACTIVE, INACTIVE or REPLACED.");
            }

            previews.add(MeterImportPreviewRowResponse.of(rowNumber, row.houseId(), row.roomId(),
                    row.utilityTypeId(), meterNumber, row.installedAt(), row.status(), errors, warnings));
        }
        return previews;
    }

    // ------------------------------------------------------------------
    // Authorization + parsing helpers
    // ------------------------------------------------------------------

    private Long resolveOwnerId(Long houseId, Long currentUserId) {
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            House house = houseRepository.findById(houseId).orElse(null);
            if (house == null) {
                return null;
            }
            if (!userHouseAccessRepository.existsByUserIdAndHouseId(currentUserId, houseId)) {
                return null;
            }
            return house.getOwner().getId();
        }
        House house = houseRepository.findByIdAndOwnerId(houseId, currentUserId).orElse(null);
        return house == null ? null : house.getOwner().getId();
    }

    private Long parseId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
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

    private UtilityMeterStatus parseStatus(String value) {
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