package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.GrantHouseAccessRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.UserHouseAccessResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseAccessNotFoundException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.HouseMapper;
import com.example.spring_boot_project_api.mapper.UserHouseAccessMapper;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.UserHouseAccessService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserHouseAccessServiceImpl implements UserHouseAccessService {

    private static final String FORBIDDEN_MESSAGE = "You do not have permission to access this resource";

    private final UserHouseAccessRepository userHouseAccessRepository;
    private final HouseRepository houseRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final HouseMapper houseMapper;
    private final UserHouseAccessMapper userHouseAccessMapper;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public UserHouseAccessResponse grantAccess(Long houseId, GrantHouseAccessRequest request) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_ACCESS_MANAGE);
        requireOwner();

        House house = getOwnedHouse(houseId);

        User targetUser = userRepository.findById(request.userId())
                .orElseThrow(() -> UserNotFoundException.forId(request.userId()));

        // Only MANAGER-role users may be assigned house access.
        if (!securityUtils.hasRole(targetUser.getId(), SecurityUtils.ROLE_MANAGER)) {
            throw new AccessDeniedException(
                    "House access can only be granted to users with the MANAGER role");
        }

        if (userHouseAccessRepository.existsByUserIdAndHouseId(targetUser.getId(), house.getId())) {
            throw new DuplicateResourceException("User " + targetUser.getUsername()
                    + " already has access to this house");
        }

        return userHouseAccessMapper.toResponse(
                userHouseAccessRepository.saveAndFlush(userHouseAccessMapper.toEntity(targetUser, house)));
    }

    @Override
    @Transactional
    public void revokeAccess(Long houseId, Long userId) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_ACCESS_MANAGE);
        requireOwner();
        getOwnedHouse(houseId);

        if (!userHouseAccessRepository.existsByUserIdAndHouseId(userId, houseId)) {
            throw HouseAccessNotFoundException.forUserAndHouse(userId, houseId);
        }

        userHouseAccessRepository.deleteByUserIdAndHouseId(userId, houseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccess(Long userId, Long houseId) {
        return userHouseAccessRepository.existsByUserIdAndHouseId(userId, houseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserHouseAccessResponse> getHouseUsers(Long houseId) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_ACCESS_VIEW);
        requireOwner();
        getOwnedHouse(houseId);

        return userHouseAccessRepository.findByHouseId(houseId).stream()
                .map(userHouseAccessMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAccessCandidates(Long houseId) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_ACCESS_VIEW);
        requireOwner();
        House house = getOwnedHouse(houseId);
        Long houseIdFinal = house.getId();

        return userRoleRepository.findUsersByRoleCode(SecurityUtils.ROLE_MANAGER).stream()
                .filter(u -> !userHouseAccessRepository.existsByUserIdAndHouseId(u.getId(), houseIdFinal))
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseResponse> getUserHouses() {
        securityUtils.requirePermission(PermissionConstants.HOUSE_ACCESS_VIEW);
        Long currentUserId = securityUtils.getCurrentUserId();

        if (securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            return houseRepository.findAllByOwnerIdOrderByCreatedAtDesc(currentUserId).stream()
                    .map(houseMapper::toResponse)
                    .toList();
        }

        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            return userHouseAccessRepository.findHousesByUserId(currentUserId).stream()
                    .map(houseMapper::toResponse)
                    .toList();
        }

        // TENANT and other roles do not use user_house_access for this feature.
        throw new AccessDeniedException(FORBIDDEN_MESSAGE);
    }

    private void requireOwner() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            throw new AccessDeniedException(FORBIDDEN_MESSAGE);
        }
    }

    private House getOwnedHouse(Long houseId) {
        Long currentUserId = securityUtils.getCurrentUserId();

        return houseRepository.findById(houseId)
                .map(house -> {
                    if (!house.getOwner().getId().equals(currentUserId)) {
                        throw new AccessDeniedException(FORBIDDEN_MESSAGE);
                    }
                    return house;
                })
                .orElseThrow(() -> HouseNotFoundException.forId(houseId));
    }
}
