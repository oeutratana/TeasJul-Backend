package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.CreateSystemDefaultRequest;
import com.example.spring_boot_project_api.dto.request.UpdateSystemDefaultRequest;
import com.example.spring_boot_project_api.dto.response.SystemDefaultResponse;
import com.example.spring_boot_project_api.dto.response.SystemDefaultViewResponse;
import com.example.spring_boot_project_api.enums.OwnerSystemDefaultStatus;
import com.example.spring_boot_project_api.enums.SystemDefaultKind;
import com.example.spring_boot_project_api.enums.SystemDefaultStatus;
import com.example.spring_boot_project_api.enums.UtilityBillingMethod;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.SystemDefaultNotFoundException;
import com.example.spring_boot_project_api.mapper.SystemDefaultMapper;
import com.example.spring_boot_project_api.model.Amenity;
import com.example.spring_boot_project_api.model.OwnerSystemDefault;
import com.example.spring_boot_project_api.model.SystemDefault;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.AmenityRepository;
import com.example.spring_boot_project_api.repository.OwnerSystemDefaultRepository;
import com.example.spring_boot_project_api.repository.SystemDefaultRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.impl.SystemDefaultServiceImpl;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class SystemDefaultServiceImplTest {

    @Mock
    private SystemDefaultRepository systemDefaultRepository;
    @Mock
    private OwnerSystemDefaultRepository ownerSystemDefaultRepository;
    @Mock
    private UtilityTypeRepository utilityTypeRepository;
    @Mock
    private AmenityRepository amenityRepository;
    @Mock
    private SystemDefaultMapper systemDefaultMapper;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private SystemDefaultServiceImpl systemDefaultService;

    private SystemDefault def(Long id, SystemDefaultKind kind, String name, String code,
            SystemDefaultStatus status) {
        return SystemDefault.builder()
                .id(id)
                .kind(kind)
                .name(name)
                .code(code)
                .unit("kWh")
                .billingMethod(UtilityBillingMethod.METERED)
                .status(status)
                .build();
    }

    private User user(Long id) {
        return User.builder().id(id).build();
    }

    private SystemDefaultResponse toResponse(SystemDefault d) {
        return new SystemDefaultResponse(d.getId(), d.getKind(), d.getName(), d.getCode(),
                d.getUnit(), d.getBillingMethod(), d.getDescription(), d.getIcon(), d.getStatus(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    private void stubToResponse() {
        when(systemDefaultMapper.toResponse(any(SystemDefault.class)))
                .thenAnswer(inv -> toResponse(inv.getArgument(0)));
    }

    private void stubToView() {
        when(systemDefaultMapper.toViewResponse(any(SystemDefault.class), any()))
                .thenAnswer(inv -> {
                    SystemDefault d = inv.getArgument(0);
                    OwnerSystemDefault a = inv.getArgument(1);
                    boolean enabled = a != null
                            && a.getStatus() == OwnerSystemDefaultStatus.ENABLED;
                    Long linkedId = a == null ? null
                            : (d.getKind() == SystemDefaultKind.UTILITY
                                    ? a.getUtilityTypeId()
                                    : a.getAmenityId());
                    return new SystemDefaultViewResponse(toResponse(d), enabled,
                            a == null ? null : a.getStatus(), linkedId);
                });
    }

    private void stubSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
    }

    private void stubOwnerActor() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
    }

    private void stubUtilityTypeRepositoriesEmpty() {
        when(utilityTypeRepository.findByOwnerIdAndCodeIgnoreCase(anyLong(), any()))
                .thenReturn(Optional.empty());
        when(utilityTypeRepository.findByOwnerIdAndNameIgnoreCase(anyLong(), any()))
                .thenReturn(Optional.empty());
    }

    private CreateSystemDefaultRequest utilityRequest() {
        return new CreateSystemDefaultRequest(SystemDefaultKind.UTILITY, "Electricity",
                "ELECTRIC", "kWh", UtilityBillingMethod.METERED, null, null);
    }

    // ------------------------------------------------------------------
    // Admin CRUD
    // ------------------------------------------------------------------

    @Test
    void createDefaultRequiresSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> systemDefaultService.createDefault(utilityRequest()))
                .isInstanceOf(AccessDeniedException.class);
        verify(systemDefaultRepository, never()).save(any());
    }

    @Test
    void createUtilityDefaultSuccess() {
        stubSuperAdmin();
        when(systemDefaultRepository.existsByKindAndCode(SystemDefaultKind.UTILITY, "ELECTRIC"))
                .thenReturn(false);
        when(systemDefaultRepository.existsByKindAndName(SystemDefaultKind.UTILITY, "Electricity"))
                .thenReturn(false);
        SystemDefault saved = def(1L, SystemDefaultKind.UTILITY, "Electricity", "ELECTRIC",
                SystemDefaultStatus.ACTIVE);
        when(systemDefaultMapper.toEntity(any(CreateSystemDefaultRequest.class)))
                .thenReturn(saved);
        when(systemDefaultRepository.save(any(SystemDefault.class))).thenReturn(saved);
        stubToResponse();

        SystemDefaultResponse result = systemDefaultService.createDefault(utilityRequest());

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.kind()).isEqualTo(SystemDefaultKind.UTILITY);
        assertThat(result.code()).isEqualTo("ELECTRIC");
    }

    @Test
    void createAmenityDefaultAllowsNullUnitAndBilling() {
        stubSuperAdmin();
        when(systemDefaultRepository.existsByKindAndCode(SystemDefaultKind.AMENITY, "WIFI"))
                .thenReturn(false);
        when(systemDefaultRepository.existsByKindAndName(SystemDefaultKind.AMENITY, "WiFi"))
                .thenReturn(false);
        SystemDefault saved = SystemDefault.builder()
                .id(2L)
                .kind(SystemDefaultKind.AMENITY)
                .name("WiFi")
                .code("WIFI")
                .description("High-speed internet")
                .icon("wifi")
                .status(SystemDefaultStatus.ACTIVE)
                .build();
        when(systemDefaultMapper.toEntity(any(CreateSystemDefaultRequest.class))).thenReturn(saved);
        when(systemDefaultRepository.save(any(SystemDefault.class))).thenReturn(saved);
        stubToResponse();

        SystemDefaultResponse result = systemDefaultService.createDefault(
                new CreateSystemDefaultRequest(SystemDefaultKind.AMENITY, "WiFi", "WIFI",
                        null, null, "High-speed internet", "wifi"));

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.icon()).isEqualTo("wifi");
        assertThat(result.unit()).isNull();
    }

    @Test
    void createUtilityDefaultMissingUnitRejected() {
        stubSuperAdmin();

        assertThatThrownBy(() -> systemDefaultService.createDefault(
                new CreateSystemDefaultRequest(SystemDefaultKind.UTILITY, "Electricity",
                        "ELECTRIC", null, UtilityBillingMethod.METERED, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit is required");
        verify(systemDefaultRepository, never()).save(any());
    }

    @Test
    void createUtilityDefaultMissingBillingRejected() {
        stubSuperAdmin();

        assertThatThrownBy(() -> systemDefaultService.createDefault(
                new CreateSystemDefaultRequest(SystemDefaultKind.UTILITY, "Electricity",
                        "ELECTRIC", "kWh", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Billing method is required");
        verify(systemDefaultRepository, never()).save(any());
    }

    @Test
    void createDefaultDuplicateCodeRejected() {
        stubSuperAdmin();
        when(systemDefaultRepository.existsByKindAndCode(SystemDefaultKind.UTILITY, "ELECTRIC"))
                .thenReturn(true);

        assertThatThrownBy(() -> systemDefaultService.createDefault(utilityRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("code");
    }

    @Test
    void createDefaultDuplicateNameRejected() {
        stubSuperAdmin();
        when(systemDefaultRepository.existsByKindAndCode(SystemDefaultKind.UTILITY, "ELECTRIC"))
                .thenReturn(false);
        when(systemDefaultRepository.existsByKindAndName(SystemDefaultKind.UTILITY, "Electricity"))
                .thenReturn(true);

        assertThatThrownBy(() -> systemDefaultService.createDefault(utilityRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("name");
    }

    @Test
    void listDefaultsRequiresSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> systemDefaultService.listDefaults(null, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listDefaultsFiltersByKindStatusAndSearch() {
        stubSuperAdmin();
        when(systemDefaultRepository.findAll()).thenReturn(List.of(
                def(1L, SystemDefaultKind.UTILITY, "Electricity", "ELECTRIC",
                        SystemDefaultStatus.ACTIVE),
                def(2L, SystemDefaultKind.UTILITY, "Water", "WATER", SystemDefaultStatus.ACTIVE),
                def(3L, SystemDefaultKind.AMENITY, "WiFi", "WIFI", SystemDefaultStatus.ACTIVE),
                def(4L, SystemDefaultKind.UTILITY, "Garbage", "GARBAGE",
                        SystemDefaultStatus.INACTIVE)));
        stubToResponse();

        List<SystemDefaultResponse> result =
                systemDefaultService.listDefaults(SystemDefaultKind.UTILITY,
                        SystemDefaultStatus.ACTIVE, "ate");

        assertThat(result).extracting(SystemDefaultResponse::code)
                .containsExactly("WATER");
    }

    @Test
    void updateDefaultSuccess() {
        stubSuperAdmin();
        SystemDefault entity = def(1L, SystemDefaultKind.UTILITY, "Electricity", "ELECTRIC",
                SystemDefaultStatus.ACTIVE);
        when(systemDefaultRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(systemDefaultRepository.save(any(SystemDefault.class))).thenReturn(entity);
        stubToResponse();

        SystemDefaultResponse result = systemDefaultService.updateDefault(1L,
                new UpdateSystemDefaultRequest("Power", "POWER", "kWh",
                        UtilityBillingMethod.METERED, null, null,
                        SystemDefaultStatus.INACTIVE));

        assertThat(result.name()).isEqualTo("Power");
        assertThat(result.code()).isEqualTo("POWER");
        assertThat(result.status()).isEqualTo(SystemDefaultStatus.INACTIVE);
    }

    @Test
    void updateDefaultDuplicateNameRejected() {
        stubSuperAdmin();
        SystemDefault entity = def(1L, SystemDefaultKind.UTILITY, "Electricity", "ELECTRIC",
                SystemDefaultStatus.ACTIVE);
        when(systemDefaultRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(systemDefaultRepository.existsByKindAndNameAndIdNot(
                SystemDefaultKind.UTILITY, "Water", 1L)).thenReturn(true);

        assertThatThrownBy(() -> systemDefaultService.updateDefault(1L,
                new UpdateSystemDefaultRequest("Water", null, null, null, null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(systemDefaultRepository, never()).save(any());
    }

    @Test
    void updateDefaultNotFoundThrows() {
        stubSuperAdmin();
        when(systemDefaultRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> systemDefaultService.updateDefault(99L,
                new UpdateSystemDefaultRequest("X", "X", null, null, null, null, null)))
                .isInstanceOf(SystemDefaultNotFoundException.class);
    }

    @Test
    void updateDefaultStatusSetsStatus() {
        stubSuperAdmin();
        SystemDefault entity = def(1L, SystemDefaultKind.UTILITY, "Electricity", "ELECTRIC",
                SystemDefaultStatus.ACTIVE);
        when(systemDefaultRepository.findById(1L)).thenReturn(Optional.of(entity));

        systemDefaultService.updateDefaultStatus(1L, SystemDefaultStatus.INACTIVE);

        assertThat(entity.getStatus()).isEqualTo(SystemDefaultStatus.INACTIVE);
        verify(systemDefaultRepository).save(entity);
    }

    // ------------------------------------------------------------------
    // Owner view
    // ------------------------------------------------------------------

    @Test
    void listDefaultsForOwnerReturnsAssignmentState() {
        stubOwnerActor();
        stubToView();
        when(securityUtils.getCurrentUser()).thenReturn(user(5L));

        SystemDefault electric = def(1L, SystemDefaultKind.UTILITY, "Electricity", "ELECTRIC",
                SystemDefaultStatus.ACTIVE);
        SystemDefault water = def(2L, SystemDefaultKind.UTILITY, "Water", "WATER",
                SystemDefaultStatus.ACTIVE);
        when(systemDefaultRepository.findAll()).thenReturn(List.of(electric, water));
        when(ownerSystemDefaultRepository.findByOwnerId(5L)).thenReturn(List.of(
                OwnerSystemDefault.builder()
                        .owner(user(5L))
                        .systemDefault(electric)
                        .utilityTypeId(10L)
                        .status(OwnerSystemDefaultStatus.ENABLED)
                        .build()));

        List<SystemDefaultViewResponse> result =
                systemDefaultService.listDefaultsForOwner(SystemDefaultKind.UTILITY);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).definition().code()).isEqualTo("ELECTRIC");
        assertThat(result.get(0).enabled()).isTrue();
        assertThat(result.get(0).linkedId()).isEqualTo(10L);
        assertThat(result.get(1).enabled()).isFalse();
        assertThat(result.get(1).linkedId()).isNull();
    }

    // ------------------------------------------------------------------
    // Owner enable/disable
    // ------------------------------------------------------------------

    @Test
    void enableUtilityCreatesOwnerCopyWhenNoMatch() {
        stubOwnerActor();
        stubToView();
        when(securityUtils.getCurrentUser()).thenReturn(user(5L));
        SystemDefault electric = def(1L, SystemDefaultKind.UTILITY, "Electricity", "ELECTRIC",
                SystemDefaultStatus.ACTIVE);
        when(systemDefaultRepository.findById(1L)).thenReturn(Optional.of(electric));
        stubUtilityTypeRepositoriesEmpty();
        when(utilityTypeRepository.save(any(UtilityType.class))).thenAnswer(inv -> {
            UtilityType t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });
        when(ownerSystemDefaultRepository.findByOwnerIdAndSystemDefault(5L, electric))
                .thenReturn(Optional.empty());
        when(ownerSystemDefaultRepository.save(any(OwnerSystemDefault.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SystemDefaultViewResponse result = systemDefaultService.enableForOwner(1L);

        ArgumentCaptor<UtilityType> typeCaptor = ArgumentCaptor.forClass(UtilityType.class);
        verify(utilityTypeRepository).save(typeCaptor.capture());
        assertThat(typeCaptor.getValue().getCode()).isEqualTo("ELECTRIC");
        assertThat(typeCaptor.getValue().getOwner().getId()).isEqualTo(5L);

        ArgumentCaptor<OwnerSystemDefault> assignmentCaptor =
                ArgumentCaptor.forClass(OwnerSystemDefault.class);
        verify(ownerSystemDefaultRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getUtilityTypeId()).isEqualTo(10L);
        assertThat(assignmentCaptor.getValue().getStatus())
                .isEqualTo(OwnerSystemDefaultStatus.ENABLED);

        assertThat(result.enabled()).isTrue();
        assertThat(result.linkedId()).isEqualTo(10L);
    }

    @Test
    void enableUtilityReusesExistingOwnerTypeByCode() {
        stubOwnerActor();
        stubToView();
        when(securityUtils.getCurrentUser()).thenReturn(user(5L));
        SystemDefault electric = def(1L, SystemDefaultKind.UTILITY, "Electricity", "ELECTRIC",
                SystemDefaultStatus.ACTIVE);
        when(systemDefaultRepository.findById(1L)).thenReturn(Optional.of(electric));
        UtilityType existing = UtilityType.builder().id(10L).owner(user(5L)).code("ELECTRIC")
                .name("Electricity").unit("kWh").build();
        when(utilityTypeRepository.findByOwnerIdAndCodeIgnoreCase(5L, "ELECTRIC"))
                .thenReturn(Optional.of(existing));
        when(ownerSystemDefaultRepository.findByOwnerIdAndSystemDefault(5L, electric))
                .thenReturn(Optional.empty());
        when(ownerSystemDefaultRepository.save(any(OwnerSystemDefault.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SystemDefaultViewResponse result = systemDefaultService.enableForOwner(1L);

        verify(utilityTypeRepository, never()).save(any());
        ArgumentCaptor<OwnerSystemDefault> assignmentCaptor =
                ArgumentCaptor.forClass(OwnerSystemDefault.class);
        verify(ownerSystemDefaultRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getUtilityTypeId()).isEqualTo(10L);
        assertThat(result.linkedId()).isEqualTo(10L);
    }

    @Test
    void enableUtilityReusesExistingOwnerTypeByName() {
        stubOwnerActor();
        stubToView();
        when(securityUtils.getCurrentUser()).thenReturn(user(5L));
        SystemDefault electric = def(1L, SystemDefaultKind.UTILITY, "Electricity", "ELEC",
                SystemDefaultStatus.ACTIVE);
        when(systemDefaultRepository.findById(1L)).thenReturn(Optional.of(electric));
        when(utilityTypeRepository.findByOwnerIdAndCodeIgnoreCase(5L, "ELEC"))
                .thenReturn(Optional.empty());
        UtilityType existing = UtilityType.builder().id(10L).owner(user(5L)).code("ELEC")
                .name("Electricity").unit("kWh").build();
        when(utilityTypeRepository.findByOwnerIdAndNameIgnoreCase(5L, "Electricity"))
                .thenReturn(Optional.of(existing));
        when(ownerSystemDefaultRepository.findByOwnerIdAndSystemDefault(5L, electric))
                .thenReturn(Optional.empty());
        when(ownerSystemDefaultRepository.save(any(OwnerSystemDefault.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        systemDefaultService.enableForOwner(1L);

        verify(utilityTypeRepository, never()).save(any());
        ArgumentCaptor<OwnerSystemDefault> assignmentCaptor =
                ArgumentCaptor.forClass(OwnerSystemDefault.class);
        verify(ownerSystemDefaultRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getUtilityTypeId()).isEqualTo(10L);
    }

    @Test
    void enableAmenityCreatesOwnerAmenityCopy() {
        stubOwnerActor();
        stubToView();
        when(securityUtils.getCurrentUser()).thenReturn(user(5L));
        SystemDefault wifi = SystemDefault.builder().id(3L).kind(SystemDefaultKind.AMENITY)
                .name("WiFi").code("WIFI").status(SystemDefaultStatus.ACTIVE).build();
        when(systemDefaultRepository.findById(3L)).thenReturn(Optional.of(wifi));
        when(amenityRepository.findByOwnerIdAndCodeIgnoreCase(5L, "WIFI"))
                .thenReturn(Optional.empty());
        when(amenityRepository.findByOwnerIdAndNameIgnoreCase(5L, "WiFi"))
                .thenReturn(Optional.empty());
        when(amenityRepository.save(any(Amenity.class))).thenAnswer(inv -> {
            Amenity a = inv.getArgument(0);
            a.setId(20L);
            return a;
        });
        when(ownerSystemDefaultRepository.findByOwnerIdAndSystemDefault(5L, wifi))
                .thenReturn(Optional.empty());
        when(ownerSystemDefaultRepository.save(any(OwnerSystemDefault.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SystemDefaultViewResponse result = systemDefaultService.enableForOwner(3L);

        ArgumentCaptor<Amenity> amenityCaptor = ArgumentCaptor.forClass(Amenity.class);
        verify(amenityRepository).save(amenityCaptor.capture());
        assertThat(amenityCaptor.getValue().getOwnerId()).isEqualTo(5L);
        assertThat(amenityCaptor.getValue().getCode()).isEqualTo("WIFI");

        ArgumentCaptor<OwnerSystemDefault> assignmentCaptor =
                ArgumentCaptor.forClass(OwnerSystemDefault.class);
        verify(ownerSystemDefaultRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getAmenityId()).isEqualTo(20L);
        assertThat(result.enabled()).isTrue();
    }

    @Test
    void enableAlreadyEnabledIsIdempotent() {
        stubOwnerActor();
        stubToView();
        when(securityUtils.getCurrentUser()).thenReturn(user(5L));
        SystemDefault electric = def(1L, SystemDefaultKind.UTILITY, "Electricity", "ELECTRIC",
                SystemDefaultStatus.ACTIVE);
        when(systemDefaultRepository.findById(1L)).thenReturn(Optional.of(electric));
        UtilityType existing = UtilityType.builder().id(10L).owner(user(5L)).code("ELECTRIC")
                .name("Electricity").unit("kWh").build();
        when(utilityTypeRepository.findByOwnerIdAndCodeIgnoreCase(5L, "ELECTRIC"))
                .thenReturn(Optional.of(existing));
        OwnerSystemDefault assignment = OwnerSystemDefault.builder()
                .owner(user(5L)).systemDefault(electric).utilityTypeId(10L)
                .status(OwnerSystemDefaultStatus.ENABLED).build();
        when(ownerSystemDefaultRepository.findByOwnerIdAndSystemDefault(5L, electric))
                .thenReturn(Optional.of(assignment));
        when(ownerSystemDefaultRepository.save(any(OwnerSystemDefault.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        systemDefaultService.enableForOwner(1L);

        verify(utilityTypeRepository, never()).save(any());
        verify(ownerSystemDefaultRepository).save(assignment);
        assertThat(assignment.getUtilityTypeId()).isEqualTo(10L);
    }

    @Test
    void enableInactiveDefaultThrows() {
        stubOwnerActor();
        SystemDefault inactive = def(1L, SystemDefaultKind.UTILITY, "Old", "OLD",
                SystemDefaultStatus.INACTIVE);
        when(systemDefaultRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> systemDefaultService.enableForOwner(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not active");
        verify(ownerSystemDefaultRepository, never()).save(any());
    }

    @Test
    void enableRequiresOwnerOrSuperAdminActor() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> systemDefaultService.enableForOwner(1L))
                .isInstanceOf(AccessDeniedException.class);
        verify(systemDefaultRepository, never()).findById(anyLong());
    }

    @Test
    void enableRequiresUtilityCreatePermission() {
        stubOwnerActor();
        when(securityUtils.getCurrentUser()).thenReturn(user(5L));
        SystemDefault electric = def(1L, SystemDefaultKind.UTILITY, "Electricity", "ELECTRIC",
                SystemDefaultStatus.ACTIVE);
        when(systemDefaultRepository.findById(1L)).thenReturn(Optional.of(electric));
        doThrow(new AccessDeniedException("denied"))
                .when(securityUtils).requirePermission(PermissionConstants.UTILITY_CREATE);

        assertThatThrownBy(() -> systemDefaultService.enableForOwner(1L))
                .isInstanceOf(AccessDeniedException.class);
        verify(ownerSystemDefaultRepository, never()).save(any());
    }

    @Test
    void disableMarksAssignmentDisabled() {
        stubOwnerActor();
        stubToView();
        when(securityUtils.getCurrentUser()).thenReturn(user(5L));
        SystemDefault electric = def(1L, SystemDefaultKind.UTILITY, "Electricity", "ELECTRIC",
                SystemDefaultStatus.ACTIVE);
        when(systemDefaultRepository.findById(1L)).thenReturn(Optional.of(electric));
        OwnerSystemDefault assignment = OwnerSystemDefault.builder()
                .owner(user(5L)).systemDefault(electric).utilityTypeId(10L)
                .status(OwnerSystemDefaultStatus.ENABLED).build();
        when(ownerSystemDefaultRepository.findByOwnerIdAndSystemDefault(5L, electric))
                .thenReturn(Optional.of(assignment));
        when(ownerSystemDefaultRepository.save(any(OwnerSystemDefault.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SystemDefaultViewResponse result = systemDefaultService.disableForOwner(1L);

        assertThat(assignment.getStatus()).isEqualTo(OwnerSystemDefaultStatus.DISABLED);
        verify(ownerSystemDefaultRepository).save(assignment);
        assertThat(result.enabled()).isFalse();
        assertThat(result.linkedId()).isEqualTo(10L);
    }

    @Test
    void disableWithNoAssignmentIsIdempotentAndKeepsDisabledState() {
        stubOwnerActor();
        stubToView();
        when(securityUtils.getCurrentUser()).thenReturn(user(5L));
        SystemDefault electric = def(1L, SystemDefaultKind.UTILITY, "Electricity", "ELECTRIC",
                SystemDefaultStatus.ACTIVE);
        when(systemDefaultRepository.findById(1L)).thenReturn(Optional.of(electric));
        when(ownerSystemDefaultRepository.findByOwnerIdAndSystemDefault(5L, electric))
                .thenReturn(Optional.empty());

        SystemDefaultViewResponse result = systemDefaultService.disableForOwner(1L);

        verify(ownerSystemDefaultRepository, never()).save(any());
        assertThat(result.enabled()).isFalse();
    }
}