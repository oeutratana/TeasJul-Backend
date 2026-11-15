package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.RoomImportRequest;
import com.example.spring_boot_project_api.dto.request.RoomImportRow;
import com.example.spring_boot_project_api.dto.request.RoomRepuest;
import com.example.spring_boot_project_api.dto.response.RoomImportPreviewRowResponse;
import com.example.spring_boot_project_api.dto.response.RoomImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.RoomImportResult;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.RoomImportValidationException;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.RoomImportService;
import com.example.spring_boot_project_api.service.RoomService;
import com.example.spring_boot_project_api.util.ImportFileParser;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * CSV/Excel import for rooms only. Rows can reference different houses, so each row is
 * validated (and authorized) individually, then all valid rows are written in one transaction.
 */
@Service
@RequiredArgsConstructor
public class RoomImportServiceImpl implements RoomImportService {

    private static final int MAX_ROWS = 500;
    private static final int MAX_ROOM_NUMBER = 50;

    private final RoomService roomService;
    private final HouseRepository houseRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public RoomImportPreviewResponse previewImport(String fileName, byte[] content) {
        return previewImport(null, fileName, content);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomImportPreviewResponse previewImport(Long houseId, String fileName, byte[] content) {
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
        List<RoomImportRow> rows = rowsFromRecords(records, houseId);
        return RoomImportPreviewResponse.of(fileName, validateRows(rows));
    }

    @Override
    @Transactional
    public RoomImportResult importRooms(RoomImportRequest request) {
        if (request == null || request.rows() == null || request.rows().isEmpty()) {
            throw new IllegalArgumentException("At least one import row is required");
        }
        if (request.rows().size() > MAX_ROWS) {
            throw new IllegalArgumentException("Too many rows (max " + MAX_ROWS + ")");
        }

        List<RoomImportRow> rowsToValidate = request.rows();
        if (request.houseId() != null) {
            String defaultHouseIdStr = String.valueOf(request.houseId());
            rowsToValidate = request.rows().stream()
                    .map(r -> new RoomImportRow(
                            defaultHouseIdStr,
                            r.roomTypeId(),
                            r.roomNumber(),
                            r.name(),
                            r.floorNumber(),
                            r.monthlyRent(),
                            r.securityDeposit(),
                            r.capacity(),
                            r.status(),
                            r.description()
                    ))
                    .toList();
        }

        List<RoomImportPreviewRowResponse> validations = validateRows(rowsToValidate);
        Map<Integer, List<String>> invalid = new HashMap<>();
        for (RoomImportPreviewRowResponse v : validations) {
            if (!v.valid()) {
                invalid.put(v.rowNumber(), v.errors());
            }
        }
        if (!invalid.isEmpty()) {
            throw new RoomImportValidationException(
                    "Some rows are invalid. Nothing was imported.", invalid);
        }

        List<String> messages = new ArrayList<>();
        int created = 0;
        for (int i = 0; i < rowsToValidate.size(); i++) {
            RoomImportRow row = rowsToValidate.get(i);
            RoomResponse saved = roomService.createRoom(toCreateRequest(row));
            created++;
            messages.add("Row " + (i + 2) + ": room " + saved.getRoomNumber() + " created.");
        }
        return RoomImportResult.of(created, messages);
    }

    // ------------------------------------------------------------------
    // Row mapping from parsed file
    // ------------------------------------------------------------------

    private List<RoomImportRow> rowsFromRecords(List<List<String>> records, Long defaultHouseId) {
        if (records.isEmpty()) {
            throw new IllegalArgumentException("No data rows found in the uploaded file");
        }
        List<String> header = records.get(0);
        boolean hasHouseIdHeader = header != null && !header.isEmpty()
                && header.get(0).trim().equalsIgnoreCase("houseid");

        List<RoomImportRow> rows = new ArrayList<>();
        for (int i = 1; i < records.size() && rows.size() < MAX_ROWS; i++) {
            rows.add(toRow(records.get(i), hasHouseIdHeader, defaultHouseId));
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("No data rows found in the uploaded file");
        }
        return rows;
    }

    private RoomImportRow toRow(List<String> record, boolean hasHouseIdHeader, Long defaultHouseId) {
        String houseIdStr;
        int offset;
        if (hasHouseIdHeader) {
            houseIdStr = defaultHouseId != null ? String.valueOf(defaultHouseId) : ImportFileParser.stringField(record, 0, 20);
            offset = 1;
        } else {
            houseIdStr = defaultHouseId != null ? String.valueOf(defaultHouseId) : null;
            offset = 0;
        }

        return RoomImportRow.of(
                houseIdStr,
                ImportFileParser.stringField(record, offset + 0, 20),
                ImportFileParser.stringField(record, offset + 1, MAX_ROOM_NUMBER),
                ImportFileParser.stringField(record, offset + 2, 100),
                ImportFileParser.stringField(record, offset + 3, 10),
                ImportFileParser.stringField(record, offset + 4, 20),
                ImportFileParser.stringField(record, offset + 5, 20),
                ImportFileParser.stringField(record, offset + 6, 10),
                ImportFileParser.stringField(record, offset + 7, 20),
                ImportFileParser.stringField(record, offset + 8, 1000));
    }

    private RoomRepuest toCreateRequest(RoomImportRow row) {
        RoomRepuest req = new RoomRepuest();
        req.setHouseId(parseId(row.houseId()));
        req.setRoomTypeId(parseId(row.roomTypeId()));
        req.setRoomNumber(row.roomNumber());
        req.setName(row.name());
        req.setFloorNumber(parseInteger(row.floorNumber()));
        BigDecimal rent = parseMoney(row.monthlyRent());
        req.setMonthlyRent(rent != null ? rent : BigDecimal.ZERO);
        BigDecimal deposit = parseMoney(row.securityDeposit());
        req.setSecurityDeposit(deposit != null ? deposit : BigDecimal.ZERO);
        Integer capacity = parseInteger(row.capacity());
        req.setCapacity(capacity != null ? capacity : 1);
        req.setStatus(parseStatus(row.status()));
        req.setDescription(row.description());
        return req;
    }

    // ------------------------------------------------------------------
    // Validation (shared by preview and confirm)
    // ------------------------------------------------------------------

    private List<RoomImportPreviewRowResponse> validateRows(List<RoomImportRow> rows) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Map<Long, Long> ownerByHouse = new HashMap<>();
        Map<Long, Set<String>> seenRoomsByHouse = new HashMap<>();

        List<RoomImportPreviewRowResponse> previews = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            RoomImportRow row = rows.get(i);
            int rowNumber = i + 2;
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

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

            Long roomTypeId = parseId(row.roomTypeId());
            if (roomTypeId == null) {
                errors.add("RoomTypeId is required.");
            } else if (ownerId != null) {
                RoomType roomType = roomTypeRepository.findById(roomTypeId).orElse(null);
                if (roomType == null) {
                    errors.add("Room type not found.");
                } else if (!ownerId.equals(roomType.getOwnerId())) {
                    errors.add("Room type does not belong to this house owner.");
                }
            }

            String roomNumber = row.roomNumber();
            if (roomNumber == null || roomNumber.isBlank()) {
                errors.add("RoomNumber is required.");
            } else if (houseId != null) {
                Set<String> seen = seenRoomsByHouse.computeIfAbsent(
                        houseId, k -> new HashSet<>(roomRepository.findByHouseId(houseId).stream()
                                .map(r -> r.getRoomNumber())
                                .toList()));
                if (!seen.add(roomNumber)) {
                    errors.add("Room number already exists in this house: " + roomNumber);
                }
            }

            BigDecimal rent = parseMoney(row.monthlyRent());
            if (row.monthlyRent() == null || row.monthlyRent().isBlank()) {
                errors.add("MonthlyRent is required.");
            } else if (rent == null) {
                errors.add("MonthlyRent is invalid.");
            } else if (rent.signum() < 0) {
                errors.add("MonthlyRent cannot be negative.");
            }

            BigDecimal deposit = parseMoney(row.securityDeposit());
            if (row.securityDeposit() != null && !row.securityDeposit().isBlank()) {
                if (deposit == null) {
                    errors.add("SecurityDeposit is invalid.");
                } else if (deposit.signum() < 0) {
                    errors.add("SecurityDeposit cannot be negative.");
                }
            }

            String capacityRaw = row.capacity();
            if (capacityRaw != null && !capacityRaw.isBlank()) {
                Integer capacity = parseInteger(capacityRaw);
                if (capacity == null) {
                    errors.add("Capacity is invalid.");
                } else if (capacity < 1) {
                    errors.add("Capacity must be at least 1.");
                }
            }

            if (row.floorNumber() != null && !row.floorNumber().isBlank()
                    && parseInteger(row.floorNumber()) == null) {
                errors.add("FloorNumber is invalid.");
            }

            String statusRaw = row.status();
            if (statusRaw != null && !statusRaw.isBlank() && parseStatus(statusRaw) == null) {
                errors.add("Invalid status. Use AVAILABLE, OCCUPIED, MAINTENANCE or RESERVED.");
            }

            previews.add(RoomImportPreviewRowResponse.of(rowNumber, row.houseId(), row.roomTypeId(),
                    roomNumber, row.name(), row.floorNumber(), row.monthlyRent(),
                    row.securityDeposit(), capacityRaw, statusRaw, row.description(),
                    errors, warnings));
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
}