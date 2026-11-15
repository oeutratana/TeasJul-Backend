package com.example.spring_boot_project_api.service.impl;

import com.example.spring_boot_project_api.dto.request.RentalContractRequest;
import com.example.spring_boot_project_api.dto.response.ContractSummaryResponse;
import com.example.spring_boot_project_api.dto.response.ExpiringContractSummary;
import com.example.spring_boot_project_api.dto.response.OwnerContractInfoResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.RentalContractNotFoundException;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.mapper.RentalContractMapper;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.OwnerSettingService;
import com.example.spring_boot_project_api.service.RentalContractService;
import com.example.spring_boot_project_api.service.NotificationService;
import com.example.spring_boot_project_api.service.RoomBookingService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalContractServiceImpl implements RentalContractService {
    private static final Logger log = LoggerFactory.getLogger(RentalContractServiceImpl.class);

    private static final int MAX_LIST_SIZE = 200;
    private static final int MAX_EXPIRING_SOON = 20;

    private final RentalContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final RentalContractMapper contractMapper;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final RoomBookingService roomBookingService;
    private final OwnerSettingService ownerSettingService;
    private final UserRepository userRepository;

    @Override
    public RentalContractResponse createContract(RentalContractRequest request) {
        securityUtils.requirePermission(PermissionConstants.CONTRACT_CREATE);
        Long currentUserId = securityUtils.getCurrentUserId();
        Room targetRoom = resolveAccessibleRoomWithLock(request.getRoomId(), currentUserId);

        if (contractRepository.existsByRoomIdAndStatus(request.getRoomId(), RentalContractStatus.ACTIVE)) {
            throw new DuplicateResourceException("Room " + targetRoom.getRoomNumber() + " is already occupied and cannot be assigned to a new contract.");
        }
        Long bookingId = bookingIdForNewContract(targetRoom);
        if (bookingId != null && contractRepository.existsByBookingId(bookingId)) {
            throw new DuplicateResourceException("The current booking for room " + targetRoom.getRoomNumber()
                    + " is already assigned to a rental contract.");
        }

        Long ownerId = targetRoom.getOwnerId();

        RentalContract contract = contractMapper.toEntity(request);
        contract.setOwnerId(ownerId);
        snapshotOwnerContractInfo(contract, ownerId);
        contract.setBookingId(bookingId);
        contract.setContractNo(generateUniqueContractNo());
        applyStatusRules(contract);
        RentalContract savedContract = contractRepository.save(contract);
        completeBookingWhenContractStarts(savedContract);
        try {
            notificationService.notifyContractExpiring(savedContract);
        } catch (RuntimeException e) {
            log.warn("Failed to create contract-expiry notification", e);
        }
        syncRoomStatus(savedContract.getRoomId());
        return contractMapper.toResponse(savedContract);
    }

    @Override
    @Transactional
    public RentalContractResponse getContractById(Long id) {
        securityUtils.requirePermission(PermissionConstants.CONTRACT_VIEW);
        expireExpiredContracts();
        return contractMapper.toResponse(getAccessibleContract(id));
    }

    @Override
    @Transactional
    public PageResponse<RentalContractResponse> getContractsByOwner(Long ownerId, String search,
            RentalContractStatus status, Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.CONTRACT_VIEW);
        expireExpiredContracts();
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean manager = securityUtils.hasRole(SecurityUtils.ROLE_MANAGER);

        Pageable capped = PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_LIST_SIZE),
                pageable.getSort());
        String q = normalize(search);

        Page<RentalContract> page;
        if (manager) {
            List<Long> houseIds = userHouseAccessRepository.findHouseIdsByUserId(currentUserId);
            if (houseIds.isEmpty()) {
                return PageResponse.of(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0L, 0);
            }
            page = contractRepository.findScopedByHouseIds(houseIds, q, status, capped);
        } else {
            page = contractRepository.findScopedByOwnerId(currentUserId, q, status, capped);
        }

        List<RentalContractResponse> content = page.getContent().stream()
                .map(contractMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional
    public List<RentalContractResponse> getContractsByRoom(Long roomId) {
        securityUtils.requirePermission(PermissionConstants.CONTRACT_VIEW);
        expireExpiredContracts();
        Long currentUserId = securityUtils.getCurrentUserId();
        Room room = resolveAccessibleRoom(roomId, currentUserId);
        return contractRepository.findByOwnerIdAndRoomId(room.getOwnerId(), roomId)
                .stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RentalContractResponse> getContractsByStatus(RentalContractStatus status, Pageable pageable) {
        return getContractsByOwner(null, null, status, pageable);
    }

    @Override
    @Transactional
    public ContractSummaryResponse getContractsSummary() {
        securityUtils.requirePermission(PermissionConstants.CONTRACT_VIEW);
        expireExpiredContracts();
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean manager = securityUtils.hasRole(SecurityUtils.ROLE_MANAGER);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        long total = 0L;
        BigDecimal activeMonthlyRent = BigDecimal.ZERO;
        List<ExpiringContractSummary> expiring;
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(60);
        PageRequest expiringPage = PageRequest.of(0, MAX_EXPIRING_SOON);

        if (manager) {
            List<Long> houseIds = userHouseAccessRepository.findHouseIdsByUserId(currentUserId);
            if (houseIds.isEmpty()) {
                return new ContractSummaryResponse(0L, Map.of(), BigDecimal.ZERO, List.of());
            }
            for (Object[] row : contractRepository.countGroupByHouseIds(houseIds)) {
                statusCounts.put(row[0].toString(), ((Number) row[1]).longValue());
            }
            long sCount = 0L;
            for (long v : statusCounts.values()) sCount += v;
            total = sCount;
            activeMonthlyRent = contractRepository.sumActiveMonthlyRentByHouseIds(houseIds);
            expiring = contractRepository.findExpiringSoonByHouseIds(houseIds, today, windowEnd, expiringPage);
        } else {
            for (Object[] row : contractRepository.countGroupByOwnerId(currentUserId)) {
                statusCounts.put(row[0].toString(), ((Number) row[1]).longValue());
            }
            for (long v : statusCounts.values()) total += v;
            activeMonthlyRent = contractRepository.sumActiveMonthlyRentByOwnerId(currentUserId);
            expiring = contractRepository.findExpiringSoonByOwnerId(currentUserId, today, windowEnd, expiringPage);
        }

        return new ContractSummaryResponse(total, statusCounts, activeMonthlyRent, expiring);
    }

    @Override
    public RentalContractResponse updateContract(Long id, RentalContractRequest request) {
        securityUtils.requirePermission(PermissionConstants.CONTRACT_UPDATE);
        RentalContract existingContract = getAccessibleContract(id);
        Long currentUserId = securityUtils.getCurrentUserId();
        Room targetRoom = resolveAccessibleRoomWithLock(request.getRoomId(), currentUserId);
        boolean roomChanged = !existingContract.getRoomId().equals(request.getRoomId());
        RentalContractStatus previousStatus = existingContract.getStatus();

        Long bookingId = existingContract.getBookingId();
        if (roomChanged) {
            if (contractRepository.existsByRoomIdAndStatus(request.getRoomId(), RentalContractStatus.ACTIVE)) {
                throw new DuplicateResourceException("Room " + targetRoom.getRoomNumber()
                        + " is already occupied and cannot be assigned to this contract.");
            }
            bookingId = bookingIdForNewContract(targetRoom);
            if (bookingId != null && contractRepository.existsByBookingId(bookingId)) {
                throw new DuplicateResourceException("The current booking for room " + targetRoom.getRoomNumber()
                        + " is already assigned to a rental contract.");
            }
        }

        Long ownerId = targetRoom.getOwnerId();
        String existingContractNo = existingContract.getContractNo();
        Long prevRoomId = existingContract.getRoomId();
        Long previousOwnerId = existingContract.getOwnerId();
        contractMapper.updateEntityFromDto(request, existingContract);
        existingContract.setOwnerId(ownerId);
        if (!Objects.equals(ownerId, previousOwnerId)) {
            // Moving a contract to another owner's room changes who signs it, so the
            // snapshot is re-read. A same-owner update keeps the original snapshot.
            snapshotOwnerContractInfo(existingContract, ownerId);
        }
        existingContract.setContractNo(existingContractNo);
        existingContract.setBookingId(bookingId);
        applyStatusRules(existingContract);
        if (existingContract.getStatus() == RentalContractStatus.ACTIVE
                && (previousStatus != RentalContractStatus.ACTIVE || roomChanged)) {
            ensureRoomCanBecomeActive(existingContract, targetRoom);
        }
        RentalContract updatedContract = contractRepository.save(existingContract);
        completeBookingWhenContractStarts(updatedContract);
        try {
            notificationService.notifyContractExpiring(updatedContract);
        } catch (RuntimeException e) {
            log.warn("Failed to create contract-expiry notification", e);
        }
        syncRoomStatus(prevRoomId);
        if (!prevRoomId.equals(updatedContract.getRoomId())) {
            syncRoomStatus(updatedContract.getRoomId());
        }
        return contractMapper.toResponse(updatedContract);
    }

    @Override
    public RentalContractResponse updateStatus(Long id, RentalContractStatus status) {
        securityUtils.requirePermission(PermissionConstants.CONTRACT_UPDATE);
        RentalContract contract = getAccessibleContract(id);
        if (status == RentalContractStatus.ACTIVE && contract.getStatus() != RentalContractStatus.ACTIVE) {
            Room room = resolveAccessibleRoomWithLock(contract.getRoomId(), securityUtils.getCurrentUserId());
            ensureRoomCanBecomeActive(contract, room);
        }
        contract.setStatus(status);
        applyStatusRules(contract);
        RentalContract saved = contractRepository.save(contract);
        completeBookingWhenContractStarts(saved);
        syncRoomStatus(contract.getRoomId());
        return contractMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public int expireExpiredContracts() {
        LocalDate today = LocalDate.now();
        List<Long> roomIds = contractRepository.findRoomIdsOfActiveExpiredContracts(
                RentalContractStatus.ACTIVE, today);
        if (roomIds.isEmpty()) {
            return 0;
        }
        int updated = contractRepository.expireActiveContractsEndedBefore(
                RentalContractStatus.ACTIVE, RentalContractStatus.EXPIRED, today);
        for (Long roomId : roomIds) {
            syncRoomStatus(roomId);
        }
        return updated;
    }

    @Override
    @Transactional
    public void applyRenewal(Long contractId, LocalDate newEndDate) {
        RentalContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> RentalContractNotFoundException.forId(contractId));
        contract.setEndDate(newEndDate);
        contract.setStatus(RentalContractStatus.ACTIVE);
        applyStatusRules(contract);
        contractRepository.save(contract);
        syncRoomStatus(contract.getRoomId());
    }

    @Override
    public void deleteContract(Long id) {
        securityUtils.requirePermission(PermissionConstants.CONTRACT_DELETE);
        RentalContract contract = getAccessibleContract(id);
        Long roomId = contract.getRoomId();
        contractRepository.delete(contract);
        syncRoomStatus(roomId);
    }

    private void syncRoomStatus(Long roomId) {
        if (roomId == null) return;
        roomRepository.findById(roomId).ifPresent(room -> {
            boolean hasActiveContract = contractRepository.existsByRoomIdAndStatus(roomId, RentalContractStatus.ACTIVE);
            if (hasActiveContract && room.getStatus() != RoomStatus.OCCUPIED) {
                room.setStatus(RoomStatus.OCCUPIED);
                roomRepository.save(room);
            } else if (!hasActiveContract && room.getStatus() == RoomStatus.OCCUPIED) {
                room.setStatus(RoomStatus.AVAILABLE);
                roomRepository.save(room);
            }
        });
    }

    /** A booking is completed only once the existing rental contract becomes active. */
    private void completeBookingWhenContractStarts(RentalContract contract) {
        if (contract.getStatus() == RentalContractStatus.ACTIVE && contract.getBookingId() != null) {
            roomBookingService.completeBookingForContract(contract.getBookingId(), contract.getRoomId());
        }
    }

    /**
     * A new contract may consume an available room or the one current booking
     * holding a booked room. All other room states are unavailable for contracts.
     */
    private Long bookingIdForNewContract(Room room) {
        if (room.getStatus() == RoomStatus.AVAILABLE) {
            return null;
        }
        if (room.getStatus() == RoomStatus.BOOKED) {
            return roomBookingService.findCurrentBookingForRoom(room.getId())
                    .map(booking -> booking.getId())
                    .orElseThrow(() -> new DuplicateResourceException(
                            "Booked room " + room.getRoomNumber() + " has no current booking."));
        }
        throw new DuplicateResourceException(
                "Room " + room.getRoomNumber() + " is unavailable and cannot be assigned to a new contract.");
    }

    private void ensureRoomCanBecomeActive(RentalContract contract, Room room) {
        if (contractRepository.existsByRoomIdAndStatus(room.getId(), RentalContractStatus.ACTIVE)) {
            throw new DuplicateResourceException("Room " + room.getRoomNumber()
                    + " is already occupied and cannot be assigned to a new contract.");
        }
        if (contract.getBookingId() == null) {
            if (room.getStatus() != RoomStatus.AVAILABLE) {
                throw new DuplicateResourceException("Room " + room.getRoomNumber()
                        + " is unavailable and cannot be activated for this contract.");
            }
            return;
        }
        if (room.getStatus() != RoomStatus.BOOKED
                || roomBookingService.findCurrentBookingForRoom(room.getId())
                        .filter(booking -> booking.getId().equals(contract.getBookingId()))
                        .isEmpty()) {
            throw new DuplicateResourceException("The booking assigned to room " + room.getRoomNumber()
                    + " is no longer current and cannot be used for this contract.");
        }
    }

    // Business contract numbers are system-generated on the backend. The existing
    // CT-MM-dd-<random hex> shape is preserved; the random suffix makes collisions
    // practically impossible and the (owner_id, contract_no) unique constraint in
    // the database is the final safety net.
    private static final DateTimeFormatter CONTRACT_NO_DATE =
            DateTimeFormatter.ofPattern("MM-dd");

    private String generateUniqueContractNo() {
        String datePart = LocalDate.now().format(CONTRACT_NO_DATE);
        while (true) {
            String candidate = "CT-" + datePart + "-" + randomHex(9);
            if (!contractRepository.existsByContractNo(candidate)) {
                return candidate;
            }
        }
    }

    private static String randomHex(int length) {
        return UUID.randomUUID().toString().replace("-", "")
                .substring(0, length);
    }

    // A contract whose end date has passed (end date is inclusive, so "today"
    // still counts as active) must never remain ACTIVE.
    private void applyStatusRules(RentalContract contract) {
        if (contract.getStatus() == RentalContractStatus.ACTIVE
                && contract.getEndDate() != null
                && contract.getEndDate().isBefore(LocalDate.now())) {
            contract.setStatus(RentalContractStatus.EXPIRED);
        }
    }

    /**
     * Freezes the owner's own contract information (Party A) onto the contract.
     *
     * <p>Read once, at creation time, from the owner's settings so that a later
     * settings change cannot rewrite a contract that was already signed. Optional
     * details the owner left blank fall back to the matching column on their user
     * account - name and phone only - so a contract never prints an empty signer.
     * Party B is not touched here; tenant data comes from the separate
     * contract-tenant assignment workflow.</p>
     */
    private void snapshotOwnerContractInfo(RentalContract contract, Long ownerId) {
        OwnerContractInfoResponse info = ownerSettingService.getContractInfo(ownerId);
        if (info == null) {
            // A missing settings record must never block signing a lease: fall
            // through to the owner account so the signer is still identified.
            info = new OwnerContractInfoResponse("", null, null, "", "", "", "", "");
        }
        User owner = userRepository.findById(ownerId).orElse(null);

        String fullName = firstNonBlank(info.fullName(), ownerFullName(owner));
        contract.setOwnerFullName(fullName);
        contract.setOwnerGender(blankToNull(info.gender() == null ? null : info.gender().name()));
        contract.setOwnerDateOfBirth(info.dateOfBirth());
        contract.setOwnerNationality(blankToNull(info.nationality()));
        contract.setOwnerNationalId(blankToNull(info.nationalId()));
        contract.setOwnerAddress(blankToNull(info.currentAddress()));
        contract.setOwnerPhone(firstNonBlank(info.phone(), owner == null ? null : owner.getPhone()));
        contract.setOwnerOccupation(blankToNull(info.occupation()));
    }

    private static String ownerFullName(User owner) {
        if (owner == null) {
            return null;
        }
        String first = owner.getFirstName() == null ? "" : owner.getFirstName().trim();
        String last = owner.getLastName() == null ? "" : owner.getLastName().trim();
        return (first + " " + last).trim();
    }

    private static String firstNonBlank(String preferred, String fallback) {
        String a = preferred == null ? "" : preferred.trim();
        return a.isEmpty() ? blankToNull(fallback) : a;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private RentalContract getAccessibleContract(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        return contractRepository.findById(id)
                .map(contract -> {
                    if (canAccess(contract.getOwnerId(), contract.getRoomId(), currentUserId)) {
                        return contract;
                    }
                    throw new AccessDeniedException(
                            "You do not have permission to access this resource");
                })
                .orElseThrow(() -> RentalContractNotFoundException.forId(id));
    }

    private Room resolveAccessibleRoom(Long roomId, Long currentUserId) {
        return roomRepository.findById(roomId)
                .map(room -> {
                    if (canAccess(room.getOwnerId(), room.getHouseId(), currentUserId)) {
                        return room;
                    }
                    throw new AccessDeniedException(
                            "You do not have permission to access this resource");
                })
                .orElseThrow(() -> RoomNotFoundException.forId(roomId));
    }

    /** Locks the room through the contract decision and status update. */
    private Room resolveAccessibleRoomWithLock(Long roomId, Long currentUserId) {
        return roomRepository.findWithLockById(roomId)
                .map(room -> {
                    if (canAccess(room.getOwnerId(), room.getHouseId(), currentUserId)) {
                        return room;
                    }
                    throw new AccessDeniedException(
                            "You do not have permission to access this resource");
                })
                .orElseThrow(() -> RoomNotFoundException.forId(roomId));
    }

    private boolean canAccess(Long ownerId, Long houseId, Long currentUserId) {
        return ownerId.equals(currentUserId)
                || (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)
                        && userHouseAccessRepository.existsByUserIdAndHouseId(currentUserId, houseId));
    }

    private static String normalize(String q) {
        return q == null ? null : q.trim();
    }
}
