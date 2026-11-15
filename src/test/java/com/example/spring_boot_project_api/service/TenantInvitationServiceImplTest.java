package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.spring_boot_project_api.dto.request.ConsumeTenantInvitationRequest;
import com.example.spring_boot_project_api.dto.request.CreateTenantInvitationRequest;
import com.example.spring_boot_project_api.dto.response.InvitationConsumeResponse;
import com.example.spring_boot_project_api.dto.response.InvitationValidationResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvitationCreateResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvitationResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.InvalidInvitationTokenException;
import com.example.spring_boot_project_api.exception.InvitationAlreadyUsedException;
import com.example.spring_boot_project_api.exception.InvitationEmailMismatchException;
import com.example.spring_boot_project_api.exception.InvitationExpiredException;
import com.example.spring_boot_project_api.exception.InvitationNotFoundException;
import com.example.spring_boot_project_api.exception.InvitationRevokedException;
import com.example.spring_boot_project_api.mapper.TenantInvitationMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseStatus;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantInvitation;
import com.example.spring_boot_project_api.model.TenantInvitationStatus;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserRole;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.repository.TenantInvitationRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.impl.TenantInvitationServiceImpl;
import com.example.spring_boot_project_api.util.InvitationTokenUtil;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class TenantInvitationServiceImplTest {

    @Mock
    private TenantInvitationRepository tenantInvitationRepository;
    @Mock
    private HouseRepository houseRepository;
    @Mock
    private UserHouseAccessRepository userHouseAccessRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecurityUtils securityUtils;
    @Spy
    private com.example.spring_boot_project_api.mapper.TenantInvitationMapper tenantInvitationMapper =
            new com.example.spring_boot_project_api.mapper.TenantInvitationMapper();
    @InjectMocks
    private TenantInvitationServiceImpl tenantInvitationService;

    private static User user(Long id, String username) {
        return User.builder().id(id).username(username).email(username + "@example.com")
                .passwordHash("h").firstName("First").lastName("Last")
                .phone("099111222").status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private static House house(Long id, User owner) {
        return House.builder().id(id).owner(owner).name("Golden House").slug("golden")
                .code("H1").address("PP").status(HouseStatus.ACTIVE).build();
    }

    private static TenantInvitation invitation(Long id, User manager, House house, String email,
            TenantInvitationStatus status, LocalDateTime expiresAt) {
        return TenantInvitation.builder()
                .id(id)
                .manager(manager)
                .house(house)
                .email(email)
                .tokenHash(InvitationTokenUtil.hashToken("rawtoken"))
                .expiresAt(expiresAt)
                .status(status)
                .build();
    }

    @Test
    void createInvitationReturnsRawTokenOnceAndStoresOnlyHash() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        when(securityUtils.getCurrentUser()).thenReturn(manager);
        when(securityUtils.hasRole("OWNER")).thenReturn(true);
        when(houseRepository.findById(10L)).thenReturn(Optional.of(house));
        when(tenantInvitationRepository.save(any(TenantInvitation.class))).thenAnswer(inv -> {
            TenantInvitation saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        TenantInvitationCreateResponse response = tenantInvitationService
                .createInvitation(new CreateTenantInvitationRequest(10L, "vannak@rental.com"));

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.token()).isNotBlank();
        assertThat(response.invitationPath()).contains(response.token());
        assertThat(response.houseId()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("vannak@rental.com");
        assertThat(response.status()).isEqualTo(TenantInvitationStatus.PENDING);
        assertThat(response.expiresAt()).isAfter(LocalDateTime.now());

        ArgumentCaptor<TenantInvitation> captor = ArgumentCaptor.forClass(TenantInvitation.class);
        verify(tenantInvitationRepository).save(captor.capture());
        TenantInvitation saved = captor.getValue();
        assertThat(saved.getTokenHash()).isEqualTo(InvitationTokenUtil.hashToken(response.token()));
        assertThat(saved.getTokenHash()).isNotEqualTo(response.token());
        assertThat(saved.getManager()).isSameAs(manager);
        assertThat(saved.getHouse()).isSameAs(house);
    }

    @Test
    void createInvitationAllowsManagerWithHouseAccess() {
        User manager = user(1L, "managerA");
        User owner = user(2L, "ownerB");
        House house = house(10L, owner);
        when(securityUtils.getCurrentUser()).thenReturn(manager);
        when(securityUtils.hasRole("OWNER")).thenReturn(false);
        when(securityUtils.hasRole("MANAGER")).thenReturn(true);
        when(houseRepository.findById(10L)).thenReturn(Optional.of(house));
        when(userHouseAccessRepository.existsByUserIdAndHouseId(1L, 10L)).thenReturn(true);
        when(tenantInvitationRepository.save(any(TenantInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantInvitationCreateResponse response = tenantInvitationService
                .createInvitation(new CreateTenantInvitationRequest(10L, "vannak@rental.com"));

        assertThat(response).isNotNull();
        verify(tenantInvitationRepository).save(any(TenantInvitation.class));
    }

    @Test
    void createInvitationDeniesTenant() {
        User tenant = user(11L, "tenantUser");
        when(securityUtils.getCurrentUser()).thenReturn(tenant);
        when(securityUtils.hasRole("OWNER")).thenReturn(false);
        when(securityUtils.hasRole("MANAGER")).thenReturn(false);

        assertThatThrownBy(() -> tenantInvitationService
                .createInvitation(new CreateTenantInvitationRequest(10L, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(tenantInvitationRepository, never()).save(any());
    }

    @Test
    void createInvitationDeniesManagerWithoutHouseAccess() {
        User manager = user(1L, "managerA");
        User owner = user(2L, "ownerB");
        House house = house(10L, owner);
        when(securityUtils.getCurrentUser()).thenReturn(manager);
        when(securityUtils.hasRole("OWNER")).thenReturn(false);
        when(securityUtils.hasRole("MANAGER")).thenReturn(true);
        when(houseRepository.findById(10L)).thenReturn(Optional.of(house));
        when(userHouseAccessRepository.existsByUserIdAndHouseId(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> tenantInvitationService
                .createInvitation(new CreateTenantInvitationRequest(10L, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(tenantInvitationRepository, never()).save(any());
    }

    @Test
    void validateReturnsValidForPendingInvitation() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.PENDING, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findByTokenHash(InvitationTokenUtil.hashToken("rawtoken")))
                .thenReturn(Optional.of(invitation));

        InvitationValidationResponse response = tenantInvitationService.validateInvitation("rawtoken");

        assertThat(response.valid()).isTrue();
        assertThat(response.reason()).isEqualTo("VALID");
        assertThat(response.email()).isEqualTo("vannak@rental.com");
        assertThat(response.houseName()).isEqualTo("Golden House");
        assertThat(response.expiresAt()).isNotNull();
    }

    @Test
    void validateReturnsInvalidTokenWhenNotFound() {
        when(tenantInvitationRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        InvitationValidationResponse response = tenantInvitationService.validateInvitation("nope");

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void validateReturnsExpiredForExpiredPendingInvitation() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.PENDING, LocalDateTime.now().minusSeconds(1));
        when(tenantInvitationRepository.findByTokenHash(InvitationTokenUtil.hashToken("rawtoken")))
                .thenReturn(Optional.of(invitation));

        InvitationValidationResponse response = tenantInvitationService.validateInvitation("rawtoken");

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVITATION_EXPIRED");
        verify(tenantInvitationRepository).save(invitation);
        assertThat(invitation.getStatus()).isEqualTo(TenantInvitationStatus.EXPIRED);
    }

    @Test
    void validateReturnsAlreadyUsedForUsedInvitation() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.USED, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findByTokenHash(InvitationTokenUtil.hashToken("rawtoken")))
                .thenReturn(Optional.of(invitation));

        InvitationValidationResponse response = tenantInvitationService.validateInvitation("rawtoken");

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVITATION_ALREADY_USED");
    }

    @Test
    void validateReturnsRevokedForRevokedInvitation() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.REVOKED, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findByTokenHash(InvitationTokenUtil.hashToken("rawtoken")))
                .thenReturn(Optional.of(invitation));

        InvitationValidationResponse response = tenantInvitationService.validateInvitation("rawtoken");

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVITATION_REVOKED");
    }

    @Test
    void consumeCreatesUserRoleAndPendingTenantAndMarksUsed() {
        User owner = user(2L, "ownerB");
        User manager = user(1L, "managerA");
        House house = house(10L, owner);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.PENDING, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findByTokenHash(InvitationTokenUtil.hashToken("rawtoken")))
                .thenReturn(Optional.of(invitation));
        when(userRepository.existsByUsername("vannak")).thenReturn(false);
        when(userRepository.existsByEmail("vannak@rental.com")).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndPhone(2L, "012345678")).thenReturn(false);
        when(tenantInvitationRepository.markUsedIfPendingAndNotExpired(eq(5L), any(), any()))
                .thenReturn(1);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-123");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(50L);
            return u;
        });
        Role role = Role.builder().id(3L).code("TENANT").name("Tenant").build();
        when(roleRepository.findByCode("TENANT")).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUserIdAndRoleId(50L, 3L)).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndTenantCode(eq(2L), anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(9L);
            return t;
        });

        InvitationConsumeResponse response = tenantInvitationService.consumeInvitation(
                new ConsumeTenantInvitationRequest("rawtoken", "vannak", "vannak@rental.com",
                        "Password123", "Vannak", "Sok", "012345678"));

        assertThat(response.success()).isTrue();
        assertThat(response.tenantId()).isEqualTo(9L);
        assertThat(response.username()).isEqualTo("vannak");
        assertThat(response.email()).isEqualTo("vannak@rental.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-123");

        ArgumentCaptor<UserRole> roleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRole()).isSameAs(role);

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getStatus()).isEqualTo(TenantStatus.PENDING);
        assertThat(tenantCaptor.getValue().getOwner()).isSameAs(owner);
        assertThat(tenantCaptor.getValue().getUser().getId()).isEqualTo(50L);
        assertThat(tenantCaptor.getValue().getEmail()).isEqualTo("vannak@rental.com");
    }

    @Test
    void consumeRejectsInvalidToken() {
        when(tenantInvitationRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantInvitationService.consumeInvitation(
                new ConsumeTenantInvitationRequest("bad", "vannak", "vannak@rental.com",
                        "Password123", "Vannak", "Sok", "012345678")))
                .isInstanceOf(InvalidInvitationTokenException.class);
    }

    @Test
    void consumeRejectsExpiredInvitation() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.PENDING, LocalDateTime.now().minusSeconds(1));
        when(tenantInvitationRepository.findByTokenHash(InvitationTokenUtil.hashToken("rawtoken")))
                .thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> tenantInvitationService.consumeInvitation(
                new ConsumeTenantInvitationRequest("rawtoken", "vannak", "vannak@rental.com",
                        "Password123", "Vannak", "Sok", "012345678")))
                .isInstanceOf(InvitationExpiredException.class);
    }

    @Test
    void consumeRejectsRevokedInvitation() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.REVOKED, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findByTokenHash(InvitationTokenUtil.hashToken("rawtoken")))
                .thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> tenantInvitationService.consumeInvitation(
                new ConsumeTenantInvitationRequest("rawtoken", "vannak", "vannak@rental.com",
                        "Password123", "Vannak", "Sok", "012345678")))
                .isInstanceOf(InvitationRevokedException.class);
    }

    @Test
    void consumeRejectsUsedInvitation() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.USED, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findByTokenHash(InvitationTokenUtil.hashToken("rawtoken")))
                .thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> tenantInvitationService.consumeInvitation(
                new ConsumeTenantInvitationRequest("rawtoken", "vannak", "vannak@rental.com",
                        "Password123", "Vannak", "Sok", "012345678")))
                .isInstanceOf(InvitationAlreadyUsedException.class);
    }

    @Test
    void consumeRejectsEmailMismatch() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation invitation = invitation(5L, manager, house, "invited@rental.com",
                TenantInvitationStatus.PENDING, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findByTokenHash(InvitationTokenUtil.hashToken("rawtoken")))
                .thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> tenantInvitationService.consumeInvitation(
                new ConsumeTenantInvitationRequest("rawtoken", "vannak", "other@rental.com",
                        "Password123", "Vannak", "Sok", "012345678")))
                .isInstanceOf(InvitationEmailMismatchException.class);
    }

    @Test
    void consumeAllowsExternalEmailForTenant() {
        User manager = user(1L, "managerA");
        User owner = user(2L, "ownerB");
        House house = house(10L, owner);
        TenantInvitation invitation = invitation(5L, manager, house, "john.doe@gmail.com",
                TenantInvitationStatus.PENDING, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findByTokenHash(InvitationTokenUtil.hashToken("rawtoken")))
                .thenReturn(Optional.of(invitation));
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@gmail.com")).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndPhone(2L, "012345678")).thenReturn(false);
        when(tenantInvitationRepository.markUsedIfPendingAndNotExpired(eq(5L), any(), any()))
                .thenReturn(1);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-123");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(60L);
            return u;
        });
        Role role = Role.builder().id(3L).code("TENANT").name("Tenant").build();
        when(roleRepository.findByCode("TENANT")).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUserIdAndRoleId(60L, 3L)).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndTenantCode(eq(2L), anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        InvitationConsumeResponse response = tenantInvitationService.consumeInvitation(
                new ConsumeTenantInvitationRequest("rawtoken", "johndoe", "john.doe@gmail.com",
                        "Password123", "John", "Doe", "012345678"));

        assertThat(response.success()).isTrue();
        assertThat(response.email()).isEqualTo("john.doe@gmail.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void consumeRejectsDuplicateUsername() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.PENDING, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findByTokenHash(InvitationTokenUtil.hashToken("rawtoken")))
                .thenReturn(Optional.of(invitation));
        when(userRepository.existsByUsername("vannak")).thenReturn(true);

        assertThatThrownBy(() -> tenantInvitationService.consumeInvitation(
                new ConsumeTenantInvitationRequest("rawtoken", "vannak", "vannak@rental.com",
                        "Password123", "Vannak", "Sok", "012345678")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void consumeRejectsDuplicateEmail() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.PENDING, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findByTokenHash(InvitationTokenUtil.hashToken("rawtoken")))
                .thenReturn(Optional.of(invitation));
        when(userRepository.existsByUsername("vannak")).thenReturn(false);
        when(userRepository.existsByEmail("vannak@rental.com")).thenReturn(true);

        assertThatThrownBy(() -> tenantInvitationService.consumeInvitation(
                new ConsumeTenantInvitationRequest("rawtoken", "vannak", "vannak@rental.com",
                        "Password123", "Vannak", "Sok", "012345678")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void consumeLoserOfAtomicClaimThrowsAlreadyUsed() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.PENDING, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findByTokenHash(InvitationTokenUtil.hashToken("rawtoken")))
                .thenReturn(Optional.of(invitation));
        when(userRepository.existsByUsername("vannak")).thenReturn(false);
        when(userRepository.existsByEmail("vannak@rental.com")).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndPhone(1L, "012345678")).thenReturn(false);
        when(tenantInvitationRepository.markUsedIfPendingAndNotExpired(eq(5L), any(), any()))
                .thenReturn(0);

        assertThatThrownBy(() -> tenantInvitationService.consumeInvitation(
                new ConsumeTenantInvitationRequest("rawtoken", "vannak", "vannak@rental.com",
                        "Password123", "Vannak", "Sok", "012345678")))
                .isInstanceOf(InvitationAlreadyUsedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void revokeByCreatorMarksRevoked() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.PENDING, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findById(5L)).thenReturn(Optional.of(invitation));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(tenantInvitationRepository.save(invitation)).thenReturn(invitation);

        TenantInvitationResponse response = tenantInvitationService.revokeInvitation(5L);

        assertThat(response.status()).isEqualTo(TenantInvitationStatus.REVOKED);
        assertThat(invitation.getStatus()).isEqualTo(TenantInvitationStatus.REVOKED);
    }

    @Test
    void revokeDeniesCrossManagerWithoutAccess() {
        User manager = user(1L, "managerA");
        User owner = user(2L, "ownerB");
        House house = house(10L, owner);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.PENDING, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findById(5L)).thenReturn(Optional.of(invitation));
        when(securityUtils.getCurrentUserId()).thenReturn(7L);
        when(userHouseAccessRepository.existsByUserIdAndHouseId(7L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> tenantInvitationService.revokeInvitation(5L))
                .isInstanceOf(AccessDeniedException.class);
        verify(tenantInvitationRepository, never()).save(any());
    }

    @Test
    void revokeRejectsUsedInvitation() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation invitation = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.USED, LocalDateTime.now().plusMinutes(5));
        when(tenantInvitationRepository.findById(5L)).thenReturn(Optional.of(invitation));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        assertThatThrownBy(() -> tenantInvitationService.revokeInvitation(5L))
                .isInstanceOf(InvitationAlreadyUsedException.class);
    }

    @Test
    void revokeThrowsNotFoundForUnknownId() {
        when(tenantInvitationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantInvitationService.revokeInvitation(404L))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void getMyInvitationsReturnsOnlyOwnedInvitations() {
        User manager = user(1L, "managerA");
        House house = house(10L, manager);
        TenantInvitation pending = invitation(5L, manager, house, "vannak@rental.com",
                TenantInvitationStatus.PENDING, LocalDateTime.now().plusMinutes(5));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(tenantInvitationRepository.findAllVisibleByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(pending));

        List<TenantInvitationResponse> responses = tenantInvitationService.getMyInvitations();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(5L);
        assertThat(responses.get(0).houseName()).isEqualTo("Golden House");
        assertThat(responses.get(0).email()).isEqualTo("vannak@rental.com");
    }
}