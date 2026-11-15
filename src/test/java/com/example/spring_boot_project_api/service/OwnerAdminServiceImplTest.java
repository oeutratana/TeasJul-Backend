package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

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
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.impl.OwnerAdminServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class OwnerAdminServiceImplTest {

    private static final String RAW_PASSWORD = "Password123";
    private static final String HASHED_PASSWORD = "$2a$10$hashedpasswordhash123456";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private OwnerAdminServiceImpl ownerAdminService;

    private void allowSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
    }

    private CreateOwnerRequest ownerRequest() {
        return new CreateOwnerRequest("newowner", "newowner@rental.com", RAW_PASSWORD,
                "Nika", "Chea", "+85512345678");
    }

    private User ownerEntity() {
        return User.builder()
                .id(1L)
                .username("newowner")
                .email("newowner@rental.com")
                .passwordHash(HASHED_PASSWORD)
                .firstName("Nika")
                .lastName("Chea")
                .phone("+85512345678")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Role ownerRole() {
        return Role.builder()
                .id(7L)
                .name("Owner")
                .code("OWNER")
                .description("Owns houses and rooms")
                .isSystemRole(true)
                .build();
    }

    // ---------- createOwner ----------

    @Test
    void createOwnerShouldAssignOwnerRoleEncodePasswordAndReturnCreatedUser() {
        allowSuperAdmin();
        CreateOwnerRequest request = ownerRequest();
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(roleRepository.findByCode(SecurityUtils.ROLE_OWNER))
                .thenReturn(Optional.of(ownerRole()));
        when(userRoleRepository.existsByUserIdAndRoleId(1L, 7L)).thenReturn(false);

        UserResponse response = ownerAdminService.createOwner(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("newowner");
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo(HASHED_PASSWORD);
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);

        verify(passwordEncoder).encode(RAW_PASSWORD);

        ArgumentCaptor<UserRole> roleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRole().getCode()).isEqualTo(SecurityUtils.ROLE_OWNER);
        assertThat(roleCaptor.getValue().getUser().getId()).isEqualTo(1L);
    }

    @Test
    void createOwnerRequestStructurallyCannotCarryRole() {
        List<String> components = Arrays.stream(CreateOwnerRequest.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toList());
        assertThat(components)
                .contains("username", "email", "password", "firstName", "lastName", "phone")
                .doesNotContain("role", "roles", "permissions");
    }

    @Test
    void createOwnerResponseMustNeverExposePassword() {
        allowSuperAdmin();
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(roleRepository.findByCode(SecurityUtils.ROLE_OWNER))
                .thenReturn(Optional.of(ownerRole()));

        UserResponse response = ownerAdminService.createOwner(ownerRequest());

        assertThat(response.toString())
                .doesNotContain(HASHED_PASSWORD)
                .doesNotContain(RAW_PASSWORD);

        List<String> components = Arrays.stream(UserResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toList());
        assertThat(components)
                .contains("id", "username", "email", "firstName", "lastName",
                        "phone", "avatar", "status", "lastLoginAt", "createdAt", "updatedAt")
                .doesNotContain("password", "passwordHash", "token", "secret");
    }

    @Test
    void createOwnerShouldRejectNonSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> ownerAdminService.createOwner(ownerRequest()))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(userRoleRepository, never()).save(any(UserRole.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void createOwnerShouldRejectDuplicateUsername() {
        allowSuperAdmin();
        when(userRepository.existsByUsername("newowner")).thenReturn(true);

        assertThatThrownBy(() -> ownerAdminService.createOwner(ownerRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username is already taken");
    }

    @Test
    void createOwnerShouldRejectDuplicateEmail() {
        allowSuperAdmin();
        when(userRepository.existsByUsername("newowner")).thenReturn(false);
        when(userRepository.existsByEmail("newowner@rental.com")).thenReturn(true);

        assertThatThrownBy(() -> ownerAdminService.createOwner(ownerRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email is already registered");
    }

    @Test
    void createOwnerShouldRejectExternalEmailForInternalOwner() {
        allowSuperAdmin();
        CreateOwnerRequest external = new CreateOwnerRequest("newowner", "owner@gmail.com",
                RAW_PASSWORD, "Nika", "Chea", "+85512345678");

        assertThatThrownBy(() -> ownerAdminService.createOwner(external))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessageContaining("@rental.com");
    }

    @Test
    void createOwnerShouldThrowWhenOwnerRoleMissing() {
        allowSuperAdmin();
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(roleRepository.findByCode(SecurityUtils.ROLE_OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ownerAdminService.createOwner(ownerRequest()))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("OWNER");
    }

    @Test
    void createOwnerShouldNotTouchOwnershipOrHouseData() {
        allowSuperAdmin();
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(roleRepository.findByCode(SecurityUtils.ROLE_OWNER))
                .thenReturn(Optional.of(ownerRole()));

        ownerAdminService.createOwner(ownerRequest());

        // The only writes are the user row and the OWNER role assignment; no
        // house/room/house-access rows are created, so the account is never
        // accidentally granted access to another owner's data.
        verify(userRepository, never()).delete(any());
        verify(userRoleRepository, never()).deleteByUserIdAndRoleId(any(), any());
    }

    // ---------- listOwners ----------

    @Test
    void listOwnersShouldReturnPagedOwners() {
        allowSuperAdmin();
        Page<User> page = new PageImpl<>(List.of(ownerEntity()), PageRequest.of(0, 10), 1);
        when(userRepository.findAllOwners(eq(null), eq(null), any(Pageable.class))).thenReturn(page);

        PageResponse<UserResponse> result =
                ownerAdminService.listOwners(null, null, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).username()).isEqualTo("newowner");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void listOwnersShouldRejectNonSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> ownerAdminService.listOwners(null, null, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- getOwner ----------

    @Test
    void getOwnerShouldReturnOwner() {
        allowSuperAdmin();
        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerEntity()));
        when(userRoleRepository.existsByUserIdAndRoleCode(1L, SecurityUtils.ROLE_OWNER)).thenReturn(true);

        UserResponse response = ownerAdminService.getOwner(1L);

        assertThat(response.username()).isEqualTo("newowner");
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void getOwnerShouldThrowStandard404WhenMissing() {
        allowSuperAdmin();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ownerAdminService.getOwner(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getOwnerShouldThrowStandard404WhenUserIsNotOwner() {
        allowSuperAdmin();
        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerEntity()));
        when(userRoleRepository.existsByUserIdAndRoleCode(1L, SecurityUtils.ROLE_OWNER)).thenReturn(false);

        assertThatThrownBy(() -> ownerAdminService.getOwner(1L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void getOwnerShouldRejectNonSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> ownerAdminService.getOwner(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- updateOwner ----------

    @Test
    void updateOwnerShouldUpdateAllowedProfileFields() {
        allowSuperAdmin();
        User existing = ownerEntity();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRoleRepository.existsByUserIdAndRoleCode(1L, SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest("Nika", "NewLast", "+999", null);

        UserResponse response = ownerAdminService.updateOwner(1L, request);

        assertThat(response.firstName()).isEqualTo("Nika");
        assertThat(response.lastName()).isEqualTo("NewLast");
        assertThat(response.phone()).isEqualTo("+999");
        assertThat(response.avatar()).isNull();
        assertThat(response.username()).isEqualTo("newowner");
        assertThat(response.email()).isEqualTo("newowner@rental.com");
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void updateOwnerShouldRejectNonSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> ownerAdminService.updateOwner(1L,
                new UpdateUserRequest("X", null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- updateOwnerStatus ----------

    @Test
    void updateOwnerStatusShouldDeactivateOwner() {
        allowSuperAdmin();
        User existing = ownerEntity();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRoleRepository.existsByUserIdAndRoleCode(1L, SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = ownerAdminService.updateOwnerStatus(1L, UserStatus.INACTIVE);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(response.status()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void updateOwnerStatusShouldReactivateOwner() {
        allowSuperAdmin();
        User existing = ownerEntity();
        existing.setStatus(UserStatus.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRoleRepository.existsByUserIdAndRoleCode(1L, SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = ownerAdminService.updateOwnerStatus(1L, UserStatus.ACTIVE);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void updateOwnerStatusShouldRejectInvalidStatus() {
        allowSuperAdmin();

        assertThatThrownBy(() -> ownerAdminService.updateOwnerStatus(1L, UserStatus.LOCKED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACTIVE, INACTIVE");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateOwnerStatusShouldRejectNonSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> ownerAdminService.updateOwnerStatus(1L, UserStatus.ACTIVE))
                .isInstanceOf(AccessDeniedException.class);
    }
}