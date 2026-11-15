package com.example.spring_boot_project_api.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ConsumeTenantInvitationRequest;
import com.example.spring_boot_project_api.dto.request.CreateTenantInvitationRequest;
import com.example.spring_boot_project_api.dto.response.InvitationConsumeResponse;
import com.example.spring_boot_project_api.dto.response.InvitationValidationResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvitationCreateResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvitationResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.InvalidInvitationTokenException;
import com.example.spring_boot_project_api.exception.InvitationAlreadyUsedException;
import com.example.spring_boot_project_api.exception.InvitationEmailMismatchException;
import com.example.spring_boot_project_api.exception.InvitationExpiredException;
import com.example.spring_boot_project_api.exception.InvitationNotFoundException;
import com.example.spring_boot_project_api.exception.InvitationRevokedException;
import com.example.spring_boot_project_api.exception.RoleNotFoundException;
import com.example.spring_boot_project_api.mapper.TenantInvitationMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantInvitation;
import com.example.spring_boot_project_api.model.TenantInvitationStatus;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserRole;
import com.example.spring_boot_project_api.model.UserRoleId;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.repository.TenantInvitationRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.TenantInvitationService;
import com.example.spring_boot_project_api.util.InvitationTokenUtil;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantInvitationServiceImpl implements TenantInvitationService {

    private static final long INVITATION_VALIDITY_SECONDS = 300L;

    private static final String REASON_VALID = "VALID";
    private static final String REASON_INVALID_TOKEN = "INVALID_TOKEN";
    private static final String REASON_ALREADY_USED = "INVITATION_ALREADY_USED";
    private static final String REASON_EXPIRED = "INVITATION_EXPIRED";
    private static final String REASON_REVOKED = "INVITATION_REVOKED";

    private static final String TENANT_CODE_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TENANT_CODE_LENGTH = 8;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TenantInvitationRepository tenantInvitationRepository;
    private final HouseRepository houseRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final TenantInvitationMapper tenantInvitationMapper;

    @Override
    @Transactional
    public TenantInvitationCreateResponse createInvitation(CreateTenantInvitationRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            throw new AccessDeniedException(
                    "Only owners and managers can issue tenant invitations");
        }

        House house = houseRepository.findById(request.houseId())
                .orElseThrow(() -> HouseNotFoundException.forId(request.houseId()));

        Long userId = currentUser.getId();
        boolean canManage = house.getOwner().getId().equals(userId)
                || userHouseAccessRepository.existsByUserIdAndHouseId(userId, house.getId());
        if (!canManage) {
            throw new AccessDeniedException(
                    "You do not have permission to invite tenants for this house");
        }

        String rawToken = InvitationTokenUtil.generateToken();
        TenantInvitation invitation = TenantInvitation.builder()
                .manager(currentUser)
                .house(house)
                .email(normalizeEmail(request.email()))
                .tokenHash(InvitationTokenUtil.hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusSeconds(INVITATION_VALIDITY_SECONDS))
                .status(TenantInvitationStatus.PENDING)
                .build();
        invitation = tenantInvitationRepository.save(invitation);

        return tenantInvitationMapper.toCreateResponse(invitation, rawToken,
                "/tenant/invite?token=" + rawToken);
    }

    @Override
    @Transactional
    public List<TenantInvitationResponse> getMyInvitations() {
        Long userId = securityUtils.getCurrentUserId();
        List<TenantInvitation> invitations = tenantInvitationRepository
                .findAllVisibleByUserIdOrderByCreatedAtDesc(userId);
        invitations.forEach(this::markExpiredIfNeeded);
        return invitations.stream()
                .map(tenantInvitationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public InvitationValidationResponse validateInvitation(String token) {
        if (token == null || token.isBlank()) {
            return invalid(REASON_INVALID_TOKEN);
        }
        return tenantInvitationRepository
                .findByTokenHash(InvitationTokenUtil.hashToken(token.trim()))
                .map(this::buildValidationResponse)
                .orElseGet(() -> invalid(REASON_INVALID_TOKEN));
    }

    @Override
    @Transactional
    public InvitationConsumeResponse consumeInvitation(ConsumeTenantInvitationRequest request) {
        TenantInvitation invitation = tenantInvitationRepository
                .findByTokenHash(InvitationTokenUtil.hashToken(request.token().trim()))
                .orElseThrow(() -> new InvalidInvitationTokenException("Invalid invitation token"));

        markExpiredIfNeeded(invitation);
        switch (invitation.getStatus()) {
            case EXPIRED:
                throw new InvitationExpiredException("Invitation has expired");
            case USED:
                throw new InvitationAlreadyUsedException("Invitation has already been used");
            case REVOKED:
                throw new InvitationRevokedException("Invitation has been revoked");
            default:
                break;
        }

if (invitation.getEmail() != null
                && !invitation.getEmail().equalsIgnoreCase(request.email().trim())) {
            throw new InvitationEmailMismatchException(
                    "The email you entered does not match the invited email");
        }

        if (userRepository.existsByUsername(request.username().trim())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email().trim())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User owner = invitation.getHouse().getOwner();
        if (tenantRepository.existsByOwnerIdAndPhone(owner.getId(), request.phone().trim())) {
            throw new DuplicateResourceException(
                    "A tenant with this phone number already exists for the house owner");
        }

        // Atomic, race-safe claim: exactly one concurrent caller may proceed.
        LocalDateTime now = LocalDateTime.now();
        int claimed = tenantInvitationRepository
                .markUsedIfPendingAndNotExpired(invitation.getId(), now, now);
        if (claimed == 0) {
            markExpiredIfNeeded(invitation);
            if (invitation.getStatus() == TenantInvitationStatus.PENDING) {
                throw new InvitationAlreadyUsedException("Invitation has already been used");
            }
            if (invitation.getStatus() == TenantInvitationStatus.EXPIRED) {
                throw new InvitationExpiredException("Invitation has expired");
            }
            throw new InvitationRevokedException("Invitation is no longer valid");
        }

        User user = User.builder()
                .username(request.username().trim())
                .email(request.email().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phone(request.phone().trim())
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        assignTenantRole(user);

        Tenant tenant = Tenant.builder()
                .user(user)
                .owner(owner)
                .house(invitation.getHouse())
                .invitedBy(invitation.getManager())
                .tenantCode(generateUniqueTenantCode(owner.getId()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phone(request.phone().trim())
                .email(request.email().trim())
                .status(TenantStatus.PENDING)
                .build();
        tenant = tenantRepository.save(tenant);

        return new InvitationConsumeResponse(true, tenant.getId(), user.getUsername(),
                user.getEmail(), "Invitation accepted");
    }

    @Override
    @Transactional
    public TenantInvitationResponse revokeInvitation(Long id) {
        TenantInvitation invitation = tenantInvitationRepository.findById(id)
                .orElseThrow(() -> InvitationNotFoundException.forId(id));
        assertCanManageInvitation(invitation);

        if (invitation.getStatus() == TenantInvitationStatus.USED) {
            throw new InvitationAlreadyUsedException(
                    "Cannot revoke an invitation that has already been used");
        }
        if (invitation.getStatus() != TenantInvitationStatus.REVOKED) {
            invitation.setStatus(TenantInvitationStatus.REVOKED);
            invitation = tenantInvitationRepository.save(invitation);
        }
        return tenantInvitationMapper.toResponse(invitation);
    }

    private void assertCanManageInvitation(TenantInvitation invitation) {
        Long userId = securityUtils.getCurrentUserId();
        boolean authorized = invitation.getManager().getId().equals(userId)
                || invitation.getHouse().getOwner().getId().equals(userId)
                || userHouseAccessRepository.existsByUserIdAndHouseId(
                        userId, invitation.getHouse().getId());
        if (!authorized) {
            throw new AccessDeniedException(
                    "You do not have permission to manage this invitation");
        }
    }

    private InvitationValidationResponse buildValidationResponse(TenantInvitation invitation) {
        markExpiredIfNeeded(invitation);
        switch (invitation.getStatus()) {
            case USED:
                return invalid(REASON_ALREADY_USED);
            case REVOKED:
                return invalid(REASON_REVOKED);
            case EXPIRED:
                return invalid(REASON_EXPIRED);
            default:
                return new InvitationValidationResponse(true, REASON_VALID,
                        invitation.getEmail(),
                        invitation.getHouse().getName(),
                        invitation.getExpiresAt());
        }
    }

    private void markExpiredIfNeeded(TenantInvitation invitation) {
        if (invitation.getStatus() == TenantInvitationStatus.PENDING
                && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(TenantInvitationStatus.EXPIRED);
            tenantInvitationRepository.save(invitation);
        }
    }

    private void assignTenantRole(User user) {
        Role tenantRole = roleRepository.findByCode(SecurityUtils.ROLE_TENANT)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: TENANT"));
        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), tenantRole.getId())) {
            userRoleRepository.save(UserRole.builder()
                    .id(new UserRoleId(user.getId(), tenantRole.getId()))
                    .user(user)
                    .role(tenantRole)
                    .build());
        }
    }

    private String generateUniqueTenantCode(Long ownerId) {
        String code;
        do {
            StringBuilder sb = new StringBuilder(TENANT_CODE_LENGTH);
            for (int i = 0; i < TENANT_CODE_LENGTH; i++) {
                sb.append(TENANT_CODE_CHARSET.charAt(
                        SECURE_RANDOM.nextInt(TENANT_CODE_CHARSET.length())));
            }
            code = "TN-" + sb;
        } while (tenantRepository.existsByOwnerIdAndTenantCode(ownerId, code));
        return code;
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim();
    }

    private static InvitationValidationResponse invalid(String reason) {
        return new InvitationValidationResponse(false, reason, null, null, null);
    }
}