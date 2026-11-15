package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateOwnerRequest;
import com.example.spring_boot_project_api.dto.request.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.InvalidEmailException;
import com.example.spring_boot_project_api.exception.RoleNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserRole;
import com.example.spring_boot_project_api.model.UserRoleId;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.OwnerAdminService;
import com.example.spring_boot_project_api.util.EmailValidator;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * SUPER_ADMIN management of OWNER accounts.
 *
 * <p>Access rules (resolved server-side, never trusted from the client):</p>
 * <ul>
 *   <li>Every operation requires the SUPER_ADMIN role, guarded by an explicit
 *       role check (never the generic permission check, because OWNER is
 *       implicitly granted every permission).</li>
 *   <li>Created accounts always receive the existing OWNER role; the request
 *       carries no role field, so role escalation (e.g. to SUPER_ADMIN) is
 *       impossible.</li>
 *   <li>Passwords are never stored in plaintext: they are encoded with the
 *       existing {@code PasswordEncoder} and never exposed in responses.</li>
 *   <li>Deactivation reuses the existing {@link UserStatus} mechanism, so a
 *       deactivated owner can no longer authenticate under the existing
 *       security design while their houses/rooms/data remain intact.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OwnerAdminServiceImpl implements OwnerAdminService {

    private static final String ACCESS_DENIED_MSG =
            "You do not have permission to access this resource";

    /** Hard cap on how many owner rows can be requested in one page. */
    private static final int MAX_LIST_SIZE = 200;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createOwner(CreateOwnerRequest request) {
        requireSuperAdmin();

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already registered");
        }
        // OWNER is an internal (non-TENANT) role and, by existing convention,
        // internal accounts must use the corporate-domain login email.
        if (!EmailValidator.isOnInternalDomain(request.email())) {
            throw new InvalidEmailException(EmailValidator.internalEmailRequirement());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        assignOwnerRole(user);

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listOwners(String search, UserStatus status, Pageable pageable) {
        requireSuperAdmin();

        Pageable capped = PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_LIST_SIZE),
                pageable.getSort());
        Page<User> page = userRepository.findAllOwners(trim(search), status, capped);
        List<UserResponse> content = page.getContent().stream()
                .map(userMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getOwner(Long id) {
        requireSuperAdmin();
        return userMapper.toResponse(findOwnerEntity(id));
    }

    @Override
    @Transactional
    public UserResponse updateOwner(Long id, UpdateUserRequest request) {
        requireSuperAdmin();

        User owner = findOwnerEntity(id);
        if (request.firstName() != null) {
            owner.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            owner.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            owner.setPhone(request.phone());
        }
        if (request.avatar() != null) {
            owner.setAvatar(request.avatar());
        }
        return userMapper.toResponse(userRepository.save(owner));
    }

    @Override
    @Transactional
    public UserResponse updateOwnerStatus(Long id, UserStatus status) {
        requireSuperAdmin();

        if (status != UserStatus.ACTIVE && status != UserStatus.INACTIVE) {
            throw new IllegalArgumentException(
                    "Invalid owner status. Allowed values: ACTIVE, INACTIVE");
        }

        User owner = findOwnerEntity(id);
        owner.setStatus(status);
        return userMapper.toResponse(userRepository.save(owner));
    }

    private void requireSuperAdmin() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }
    }

    /**
     * Loads the user and guarantees it actually holds the OWNER role; a
     * non-owner id is reported with the project's standard 404 behavior.
     */
    private User findOwnerEntity(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.forId(id));
        if (!userRoleRepository.existsByUserIdAndRoleCode(id, SecurityUtils.ROLE_OWNER)) {
            throw UserNotFoundException.forId(id);
        }
        return user;
    }

    private void assignOwnerRole(User user) {
        Role ownerRole = roleRepository.findByCode(SecurityUtils.ROLE_OWNER)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + SecurityUtils.ROLE_OWNER));
        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), ownerRole.getId())) {
            userRoleRepository.save(UserRole.builder()
                    .id(new UserRoleId(user.getId(), ownerRole.getId()))
                    .user(user)
                    .role(ownerRole)
                    .build());
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}