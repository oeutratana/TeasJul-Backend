package com.example.spring_boot_project_api.util;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.RolePermissionRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves the currently authenticated user from the Spring Security context
 * (populated by the JWT filter). Ownership for owner-scoped resources must be
 * derived from this user, never from the client request.
 *
 * <p>Permission checks ({@link #hasPermission}/{@link #requirePermission}) always
 * grant the SUPER_ADMIN and OWNER roles full access so they can never be locked
 * out through permission management; every other role must hold the permission
 * on one of its assigned roles.</p>
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_TENANT = "TENANT";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /** Whether the current user has the given role code (role codes are case-sensitive). */
    public boolean hasRole(String roleCode) {
        return userRoleRepository.existsByUserIdAndRoleCode(getCurrentUserId(), roleCode);
    }

    /**
     * Whether the current user holds the OWNER role but is not a SUPER_ADMIN.
     * Such a user is restricted from accessing or managing the SUPER_ADMIN role.
     */
    public boolean isOwnerWithoutSuperAdmin() {
        return hasRole(ROLE_OWNER) && !hasRole(ROLE_SUPER_ADMIN);
    }

    /** Whether the given user has the given role code. */
    public boolean hasRole(Long userId, String roleCode) {
        return userRoleRepository.existsByUserIdAndRoleCode(userId, roleCode);
    }

    /** Whether the current user holds the given permission (OWNER always granted). */
    public boolean hasPermission(String permissionCode) {
        return hasPermission(getCurrentUserId(), permissionCode);
    }

    /** Whether the given user holds the given permission (SUPER_ADMIN and OWNER always granted). */
    public boolean hasPermission(Long userId, String permissionCode) {
        if (hasRole(userId, ROLE_SUPER_ADMIN) || hasRole(userId, ROLE_OWNER)) {
            return true;
        }
        List<Long> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return false;
        }
        return rolePermissionRepository.existsByRoleIdInAndPermission_Code(roleIds, permissionCode);
    }

    /** Throws {@link AccessDeniedException} unless the current user holds the permission. */
    public void requirePermission(String permissionCode) {
        if (!hasPermission(permissionCode)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }
}
