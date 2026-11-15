package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateUtilityReadingRequest;
import com.example.spring_boot_project_api.dto.request.MonthlyReadingBulkRequest;
import com.example.spring_boot_project_api.dto.response.ActiveRoomTenantDto;
import com.example.spring_boot_project_api.dto.response.BulkUtilityReadingResponse;
import com.example.spring_boot_project_api.dto.response.MonthlyReadingRowResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityReadingResponse;
import com.example.spring_boot_project_api.enums.ReadingStatusFilter;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.InvalidUtilityOperationException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.exception.UtilityMeterNotFoundException;
import com.example.spring_boot_project_api.exception.UtilityReadingNotFoundException;
import com.example.spring_boot_project_api.mapper.UtilityReadingMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityMeter;
import com.example.spring_boot_project_api.model.UtilityReading;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityReadingRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.UtilityReadingService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UtilityReadingServiceImpl implements UtilityReadingService {

    private final UtilityReadingRepository readingRepository;
    private final UtilityMeterRepository meterRepository;
    private final UserRepository userRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final RentalContractRepository contractRepository;
    private final UtilityReadingMapper readingMapper;
    private final SecurityUtils securityUtils;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional
    public UtilityReadingResponse createReading(CreateUtilityReadingRequest request) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_CREATE);
        UtilityMeter meter = resolveMeter(request.meterId());

        if (meter.getStatus() != UtilityMeterStatus.ACTIVE) {
            throw new InvalidUtilityOperationException(
                    "Cannot record a reading for a meter that is not active");
        }

        if (readingRepository.existsByMeterIdAndReadingDate(meter.getId(), request.readingDate())) {
            throw new DuplicateResourceException(
                    "A reading already exists for this meter on " + request.readingDate());
        }

        BigDecimal previousReading = readingRepository.findTopByMeterIdOrderByReadingDateDesc(meter.getId())
                .map(UtilityReading::getCurrentReading)
                .orElse(BigDecimal.ZERO);

        BigDecimal currentReading = request.currentReading();

        if (currentReading.compareTo(previousReading) < 0) {
            throw new InvalidUtilityOperationException(
                    "Current reading must not be less than the previous reading");
        }

        BigDecimal usage = currentReading.subtract(previousReading);

        UtilityReading reading = readingMapper.toEntity(request);
        reading.setMeter(meter);
        reading.setPreviousReading(previousReading);
        reading.setUsage(usage);

        return readingMapper.toResponse(readingRepository.save(reading));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UtilityReadingResponse> getReadings(Long meterId, LocalDate fromDate,
            LocalDate toDate, Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
        List<Long> accessibleHouseIds = houseAccessService.getAccessibleHouseIds();

        Specification<UtilityReading> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("meter").get("houseId").in(accessibleHouseIds));
            if (meterId != null) {
                predicates.add(cb.equal(root.get("meter").get("id"), meterId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("readingDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("readingDate"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return toPageResponse(readingRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public UtilityReadingResponse getReadingById(Long id) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
        UtilityReading reading = readingRepository.findById(id)
                .orElseThrow(() -> UtilityReadingNotFoundException.forId(id));

        resolveMeter(reading.getMeter().getId());

        return readingMapper.toResponse(reading);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyReadingRowResponse> getMonthlySheet(Long houseId, Integer year, Integer month,
            Integer floor, String search, ReadingStatusFilter status) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);

        List<Long> houseIds = resolveSheetHouseScope(houseId);
        if (houseIds.isEmpty()) {
            return List.of();
        }

        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = YearMonth.of(year, month).atEndOfMonth();

        Map<Long, Room> roomById = roomRepository.findByHouseIdIn(houseIds).stream()
                .collect(Collectors.toMap(Room::getId, room -> room));

        Map<Long, House> houseById = houseRepository.findAllById(houseIds).stream()
                .collect(Collectors.toMap(House::getId, house -> house));

        Map<Long, UtilityType> typeById = utilityTypeRepository
                .findByOwnerIdIn(houseAccessService.getScopedOwnerIds()).stream()
                .collect(Collectors.toMap(UtilityType::getId, type -> type));

        List<UtilityMeter> meters = meterRepository.findByHouseIdIn(houseIds).stream()
                .filter(meter -> meter.getStatus() == UtilityMeterStatus.ACTIVE)
                .toList();

        if (meters.isEmpty()) {
            return List.of();
        }

        Map<Long, List<UtilityMeter>> metersByRoom = meters.stream()
                .collect(Collectors.groupingBy(UtilityMeter::getRoomId));

        Map<Long, UtilityReading> recordedByMeter = new HashMap<>();
        for (UtilityMeter meter : meters) {
            readingRepository
                    .findTopByMeterIdAndReadingDateBetweenOrderByReadingDateDesc(
                            meter.getId(), periodStart, periodEnd)
                    .ifPresent(reading -> recordedByMeter.put(meter.getId(), reading));
        }

        // A room is "entered" only when every ACTIVE meter of the room has a
        // persisted current reading inside the billing month.
        Map<Long, Boolean> roomEntered = metersByRoom.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .allMatch(meta -> recordedByMeter.containsKey(meta.getId()))));

        Map<Long, String> tenantByRoom = resolveTenantNames(metersByRoom.keySet());

        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim().toLowerCase();

        List<MonthlyReadingRowResponse> rows = new ArrayList<>();
        for (UtilityMeter meter : meters) {
            Room room = roomById.get(meter.getRoomId());
            if (room == null) {
                continue;
            }

            if (floor != null && !Integer.valueOf(floor).equals(room.getFloorNumber())) {
                continue;
            }

            boolean entered = roomEntered.getOrDefault(room.getId(), false);
            if ((status == ReadingStatusFilter.ENTERED && !entered)
                    || (status == ReadingStatusFilter.NOT_ENTERED && entered)) {
                continue;
            }

            if (normalizedSearch != null && !matchesSearch(room, meter, tenantByRoom.get(room.getId()),
                    normalizedSearch)) {
                continue;
            }

            BigDecimal previous = readingRepository
                    .findTopByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(meter.getId(), periodStart)
                    .map(UtilityReading::getCurrentReading)
                    .orElse(BigDecimal.ZERO);

            UtilityReading recorded = recordedByMeter.get(meter.getId());
            House house = houseById.get(room.getHouseId());
            UtilityType type = typeById.get(meter.getUtilityTypeId());

            rows.add(new MonthlyReadingRowResponse(
                    room.getId(),
                    room.getRoomNumber(),
                    room.getFloorNumber(),
                    room.getHouseId(),
                    house != null ? house.getName() : null,
                    tenantByRoom.get(room.getId()),
                    meter.getId(),
                    meter.getMeterNumber(),
                    meter.getUtilityTypeId(),
                    type != null ? type.getCode() : null,
                    type != null ? type.getName() : null,
                    type != null ? type.getUnit() : null,
                    recorded != null ? recorded.getId() : null,
                    previous,
                    recorded != null ? recorded.getCurrentReading() : null,
                    recorded != null ? recorded.getUsage() : null,
                    recorded != null ? recorded.getReadingDate() : null
            ));
        }

        rows.sort(Comparator
                .comparing(MonthlyReadingRowResponse::houseName,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MonthlyReadingRowResponse::floorNumber,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MonthlyReadingRowResponse::roomNumber,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(row -> row.utilityTypeName() == null ? "" : row.utilityTypeName()));

        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getMonthlySheetFloors(Long houseId) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);

        List<Long> houseIds = resolveSheetHouseScope(houseId);
        if (houseIds.isEmpty()) {
            return List.of();
        }

        List<Long> roomIds = meterRepository.findByHouseIdIn(houseIds).stream()
                .filter(meta -> meta.getStatus() == UtilityMeterStatus.ACTIVE)
                .map(UtilityMeter::getRoomId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (roomIds.isEmpty()) {
            return List.of();
        }

        return roomRepository.findAllById(roomIds).stream()
                .map(Room::getFloorNumber)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    @Transactional
    public BulkUtilityReadingResponse createReadingsBulk(MonthlyReadingBulkRequest request) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_CREATE);
        verifyHouseAccess(request.houseId());

        LocalDate periodStart = LocalDate.of(request.billingYear(), request.billingMonth(), 1);
        LocalDate periodEnd = YearMonth.of(request.billingYear(), request.billingMonth()).atEndOfMonth();
        LocalDate readingDate = request.readingDate() != null ? request.readingDate() : periodEnd;

        if (readingDate.isBefore(periodStart) || readingDate.isAfter(periodEnd)) {
            throw new InvalidUtilityOperationException("Reading date must be within the billing month");
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<UtilityReading> toSave = new ArrayList<>();

        for (MonthlyReadingBulkRequest.MonthlyReadingRowRequest row : request.rows()) {
            UtilityMeter meter = resolveMeter(row.meterId());

            if (!meter.getHouseId().equals(request.houseId())) {
                throw new InvalidUtilityOperationException("Meter does not belong to the selected house");
            }
            if (meter.getStatus() != UtilityMeterStatus.ACTIVE) {
                throw new InvalidUtilityOperationException(
                        "Cannot record a reading for a meter that is not active");
            }

            BigDecimal previous = readingRepository
                    .findTopByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(meter.getId(), periodStart)
                    .map(UtilityReading::getCurrentReading)
                    .orElse(BigDecimal.ZERO);

            BigDecimal current = row.currentReading();
            if (current.compareTo(previous) < 0) {
                throw new InvalidUtilityOperationException(
                        "Current reading must not be less than the previous reading (meter " + meter.getMeterNumber() + ")");
            }
            BigDecimal usage = current.subtract(previous);

            Optional<UtilityReading> existing = readingRepository
                    .findTopByMeterIdAndReadingDateBetweenOrderByReadingDateDesc(meter.getId(), periodStart, periodEnd);

            if (existing.isPresent()) {
                UtilityReading reading = existing.get();
                if (reading.getCurrentReading().compareTo(current) == 0) {
                    skipped++;
                    continue;
                }
                reading.setCurrentReading(current);
                reading.setUsage(usage);
                toSave.add(reading);
                updated++;
            } else {
                toSave.add(UtilityReading.builder()
                        .meter(meter)
                        .previousReading(previous)
                        .currentReading(current)
                        .usage(usage)
                        .readingDate(readingDate)
                        .build());
                created++;
            }
        }

        if (!toSave.isEmpty()) {
            readingRepository.saveAll(toSave);
        }

        return new BulkUtilityReadingResponse(created, updated, skipped);
    }

    private List<Long> resolveSheetHouseScope(Long houseId) {
        if (houseId != null) {
            verifyHouseAccess(houseId);
            return List.of(houseId);
        }
        List<Long> accessibleHouseIds = houseAccessService.getAccessibleHouseIds();
        return accessibleHouseIds != null ? accessibleHouseIds : List.of();
    }

    private Map<Long, String> resolveTenantNames(java.util.Set<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ownerIds = houseAccessService.getScopedOwnerIds();
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = new ArrayList<>(roomIds);
        Map<Long, String> tenantByRoom = new HashMap<>();
        for (ActiveRoomTenantDto dto : contractRepository
                .findActiveTenantsByRoomIds(ids, ownerIds, LocalDate.now())) {
            if (dto == null || dto.roomId() == null || tenantByRoom.containsKey(dto.roomId())) {
                continue;
            }
            String name = (dto.firstName() == null ? "" : dto.firstName().trim())
                    + " " + (dto.lastName() == null ? "" : dto.lastName().trim());
            String normalized = name.trim();
            tenantByRoom.put(dto.roomId(), normalized.isEmpty() ? null : normalized);
        }
        return tenantByRoom;
    }

    private boolean matchesSearch(Room room, UtilityMeter meter, String tenantName, String term) {
        String roomNumber = room.getRoomNumber() == null ? "" : room.getRoomNumber().toLowerCase();
        String meterNumber = meter.getMeterNumber() == null ? "" : meter.getMeterNumber().toLowerCase();
        String tenant = tenantName == null ? "" : tenantName.toLowerCase();
        return roomNumber.contains(term) || meterNumber.contains(term) || tenant.contains(term);
    }

    private UtilityMeter resolveMeter(Long meterId) {
        Long currentOwnerId = getCurrentOwner().getId();

        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> approvedHouseIds = userHouseAccessRepository.findHouseIdsByUserId(securityUtils.getCurrentUserId());
            UtilityMeter meter = meterRepository.findById(meterId)
                    .orElseThrow(() -> UtilityMeterNotFoundException.forId(meterId));
            if (!approvedHouseIds.contains(meter.getHouseId())) {
                throw UtilityMeterNotFoundException.forId(meterId);
            }
            return meter;
        }

        return meterRepository.findByIdAndOwnerId(meterId, currentOwnerId)
                .orElseThrow(() -> UtilityMeterNotFoundException.forId(meterId));
    }

    private PageResponse<UtilityReadingResponse> toPageResponse(Page<UtilityReading> page) {
        List<UtilityReadingResponse> content = page.getContent().stream()
                .map(readingMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private void verifyHouseAccess(Long houseId) {
        Long currentUserId = securityUtils.getCurrentUserId();

        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> HouseNotFoundException.forId(houseId));

        // The house owner can always access its data (including anything created
        // by a manager assigned to the house); managers only see assigned houses.
        if (house.getOwner().getId().equals(currentUserId)) {
            return;
        }

        if (userHouseAccessRepository.existsByUserIdAndHouseId(currentUserId, houseId)) {
            return;
        }

        throw HouseNotFoundException.forId(houseId);
    }

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
