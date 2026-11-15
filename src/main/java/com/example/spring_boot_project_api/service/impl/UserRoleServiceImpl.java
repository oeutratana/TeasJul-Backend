package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.RoleResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.InvalidEmailException;
import com.example.spring_boot_project_api.exception.RoleNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.exception.UserRoleNotFoundException;
import com.example.spring_boot_project_api.mapper.RoleMapper;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserRole;
import com.example.spring_boot_project_api.model.UserRoleId;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.UserRoleService;
import com.example.spring_boot_project_api.service.UserService;
import com.example.spring_boot_project_api.util.EmailValidator;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getUserRoles(Long userId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            requireOwner();
        }
        getUserOrThrow(userId);

        boolean restrictSuperAdmin = securityUtils.isOwnerWithoutSuperAdmin();
        return userRoleRepository.findByUserId(userId).stream()
                .map(assignment -> assignment.getRole())
                .filter(role -> !(restrictSuperAdmin && isSuperAdminRole(role.getCode())))
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getCurrentUserRoles() {
        return userRoleRepository.findByUserId(securityUtils.getCurrentUserId()).stream()
                .map(assignment -> roleMapper.toResponse(assignment.getRole()))
                .toList();
    }

    @Override
    @Transactional
    public RoleResponse assignRoleToUser(Long userId, Long roleId) {
        requireOwner();
        User user = getUserOrThrow(userId);
        Role role = getRoleOrThrow(roleId);
        ensureOwnerMayManage(role);
        ensureOwnerMayNotMintOwners(role);
        // The target user must be inside the caller's ownership scope. This blocks
        // an OWNER from touching unrelated users and from modifying another OWNER
        // or a SUPER_ADMIN, which a role check alone cannot express.
        userService.requireUserManagementAccess(userId);

        if (EmailValidator.isInternalRole(role.getCode())
                && !EmailValidator.isOnInternalDomain(user.getEmail())) {
            throw new InvalidEmailException(
                    EmailValidator.internalEmailRequirement());
        }

        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new DuplicateResourceException("Role " + role.getCode()
                    + " is already assigned to user " + user.getUsername());
        }

        userRoleRepository.save(UserRole.builder()
                .id(new UserRoleId(userId, roleId))
                .user(user)
                .role(role)
                .build());

        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        requireOwner();
        getUserOrThrow(userId);
        Role role = getRoleOrThrow(roleId);
        ensureOwnerMayManage(role);
        ensureOwnerMayNotMintOwners(role);
        // Same ownership scope check as assignment: an OWNER may not edit the roles
        // of a user outside their scope, nor of another OWNER or a SUPER_ADMIN.
        userService.requireUserManagementAccess(userId);

        if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw UserRoleNotFoundException.forIds(userId, roleId);
        }

        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
    }

    private void requireOwner() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }

    /**
     * An OWNER (who is not also a SUPER_ADMIN) must never assign or remove the
     * SUPER_ADMIN role on a user. SUPER_ADMIN users keep full access.
     */
    private void ensureOwnerMayManage(Role role) {
        if (securityUtils.isOwnerWithoutSuperAdmin() && isSuperAdminRole(role.getCode())) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }

    /**
     * An OWNER (who is not also a SUPER_ADMIN) must never grant or revoke the
     * OWNER role. Without this, an OWNER could promote a user inside their own
     * scope into a peer OWNER, which is an escalation out of the caller's own
     * properties. Managers and tenants remain assignable.
     */
    private void ensureOwnerMayNotMintOwners(Role role) {
        if (securityUtils.isOwnerWithoutSuperAdmin() && isOwnerRole(role.getCode())) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }

    private boolean isSuperAdminRole(String code) {
        return SecurityUtils.ROLE_SUPER_ADMIN.equalsIgnoreCase(code);
    }

    private boolean isOwnerRole(String code) {
        return SecurityUtils.ROLE_OWNER.equalsIgnoreCase(code);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.forId(userId));
    }

    private Role getRoleOrThrow(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> RoleNotFoundException.forId(roleId));
    }
}
