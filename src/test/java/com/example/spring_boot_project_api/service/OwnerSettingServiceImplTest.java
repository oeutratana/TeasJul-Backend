package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.UpdateOwnerContractInfoRequest;
import com.example.spring_boot_project_api.dto.request.UpdateOwnerSettingsRequest;
import com.example.spring_boot_project_api.dto.request.UpdateThemeRequest;
import com.example.spring_boot_project_api.dto.response.OwnerContractInfoResponse;
import com.example.spring_boot_project_api.dto.response.OwnerSettingsResponse;
import com.example.spring_boot_project_api.dto.response.ThemeSettingsResponse;
import com.example.spring_boot_project_api.model.Gender;
import com.example.spring_boot_project_api.model.OwnerSetting;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.OwnerSettingRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.impl.OwnerSettingServiceImpl;
import com.example.spring_boot_project_api.service.impl.SecurityAuditLogger;
import com.example.spring_boot_project_api.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OwnerSettingServiceImplTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private OwnerSettingRepository ownerSettingRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private TenantRepository tenantRepository;
    @Mock private UserHouseAccessRepository userHouseAccessRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private SecurityAuditLogger securityAuditLogger;

    @InjectMocks private OwnerSettingServiceImpl ownerSettingService;

    private User owner;
    private OwnerSetting setting;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(5L).username("owner1").email("owner@test.com").build();
        setting = OwnerSetting.builder()
                .id(1L)
                .owner(owner)
                .settings(themeJson("LIGHT", "#111111", "#222222"))
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(ownerSettingRepository.findByOwnerId(5L)).thenReturn(Optional.of(setting));
        when(ownerSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(userHouseAccessRepository.findOwnerIdsByUserId(any())).thenReturn(List.of());
        when(userRoleRepository.findUsersByRoleCode(SecurityUtils.ROLE_OWNER)).thenReturn(List.of(owner));
    }

    private String themeJson(String mode, String primary, String secondary) {
        try {
            ObjectNode root = OBJECT_MAPPER.createObjectNode();
            root.put("exchangeRate", 4100.0);
            ObjectNode theme = root.putObject("theme");
            theme.put("themeMode", mode);
            theme.put("primaryColor", primary);
            theme.put("secondaryColor", secondary);
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getTheme_returnsDefaults_whenNoSettingsRowForOwner() {
        when(ownerSettingRepository.findByOwnerId(5L)).thenReturn(Optional.empty());

        ThemeSettingsResponse response = ownerSettingService.getTheme();

        assertThat(response.ownerId()).isEqualTo(5L);
        assertThat(response.themeMode()).isEqualTo("LIGHT");
        assertThat(response.primaryColor()).isEqualTo("#2563EB");
        assertThat(response.secondaryColor()).isEqualTo("#0F172A");
    }

    @Test
    void getTheme_returnsSavedThemeFromThemeBlock() {
        ThemeSettingsResponse response = ownerSettingService.getTheme();

        assertThat(response.ownerId()).isEqualTo(5L);
        assertThat(response.themeMode()).isEqualTo("LIGHT");
        assertThat(response.primaryColor()).isEqualTo("#111111");
        assertThat(response.secondaryColor()).isEqualTo("#222222");
    }

    @Test
    void getTheme_forManager_resolvesOrganizationOwnerTheme() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_TENANT)).thenReturn(false);
        when(userHouseAccessRepository.findOwnerIdsByUserId(9L)).thenReturn(List.of(5L));
        when(securityUtils.getCurrentUser()).thenReturn(
                User.builder().id(9L).username("manager1").build());

        ThemeSettingsResponse response = ownerSettingService.getTheme();

        verify(ownerSettingRepository).findByOwnerId(5L);
        assertThat(response.ownerId()).isEqualTo(5L);
        assertThat(response.themeMode()).isEqualTo("LIGHT");
    }

    @Test
    void getTheme_forTenant_resolvesOrganizationOwnerTheme() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_TENANT)).thenReturn(true);
        when(securityUtils.getCurrentUser()).thenReturn(
                User.builder().id(9L).username("tenant1").build());
        when(tenantRepository.findByUserId(9L)).thenReturn(
                Optional.of(Tenant.builder().id(10L).user(
                        User.builder().id(9L).build()).owner(owner).build()));

        ThemeSettingsResponse response = ownerSettingService.getTheme();

        verify(ownerSettingRepository).findByOwnerId(5L);
        assertThat(response.ownerId()).isEqualTo(5L);
        assertThat(response.themeMode()).isEqualTo("LIGHT");
    }

    @Test
    void getTheme_forUserWithoutOrganization_returnsDefaults() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_TENANT)).thenReturn(false);
        when(userHouseAccessRepository.findOwnerIdsByUserId(9L)).thenReturn(List.of());
        when(securityUtils.getCurrentUser()).thenReturn(
                User.builder().id(9L).username("staff1").build());

        ThemeSettingsResponse response = ownerSettingService.getTheme();

        verify(ownerSettingRepository, never()).findByOwnerId(any());
        assertThat(response.ownerId()).isNull();
        assertThat(response.primaryColor()).isEqualTo("#2563EB");
    }

    @Test
    void getTheme_fallsBackToBrandingColor_whenThemeBlockMissing() {
        OwnerSetting brandingOnly = OwnerSetting.builder()
                .id(2L)
                .owner(owner)
                .settings("{\"primaryColor\":\"#3b82f6\",\"secondaryColor\":\"#10b981\"}")
                .build();
        when(ownerSettingRepository.findByOwnerId(5L)).thenReturn(Optional.of(brandingOnly));

        ThemeSettingsResponse response = ownerSettingService.getTheme();

        assertThat(response.primaryColor()).isEqualTo("#3B82F6");
        assertThat(response.secondaryColor()).isEqualTo("#10B981");
        assertThat(response.themeMode()).isEqualTo("LIGHT");
    }

    @Test
    void updateTheme_persistsThemeAndAudits() {
        ThemeSettingsResponse response = ownerSettingService.updateTheme(
                new UpdateThemeRequest("DARK", "#ff0000", "#00ff00"));

        verify(ownerSettingRepository).save(setting);
        assertThat(setting.getSettings())
                .contains("\"themeMode\":\"DARK\"")
                .contains("\"primaryColor\":\"#FF0000\"")
                .contains("\"secondaryColor\":\"#00FF00\"");
        verify(securityAuditLogger).log(eq(5L), eq("THEME_UPDATED"), any());
        assertThat(response.themeMode()).isEqualTo("DARK");
        assertThat(response.primaryColor()).isEqualTo("#FF0000");
    }

    @Test
    void updateTheme_byNonOwner_isForbidden() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        assertThatThrownBy(() -> ownerSettingService.updateTheme(
                new UpdateThemeRequest("DARK", "#ff0000", "#00ff00")))
                .isInstanceOf(AccessDeniedException.class);

        verify(ownerSettingRepository, never()).save(any());
        verify(securityAuditLogger, never()).log(any(), any(), any());
    }

    // ---- SUPER_ADMIN shares the owner's single system theme ----

    @Test
    void getTheme_forSuperAdmin_returnsSharedOwnerTheme() {
        authenticateSuperAdmin();

        ThemeSettingsResponse response = ownerSettingService.getTheme();

        verify(ownerSettingRepository).findByOwnerId(5L);
        assertThat(response.ownerId()).isEqualTo(5L);
        assertThat(response.primaryColor()).isEqualTo("#111111");
        assertThat(response.secondaryColor()).isEqualTo("#222222");
    }

    @Test
    void updateTheme_bySuperAdmin_updatesSharedOwnerSetting() {
        authenticateSuperAdmin();

        ThemeSettingsResponse response = ownerSettingService.updateTheme(
                new UpdateThemeRequest("DARK", "#ff0000", "#00ff00"));

        verify(ownerSettingRepository).findByOwnerId(5L);
        verify(ownerSettingRepository).save(setting);
        assertThat(setting.getSettings())
                .contains("\"themeMode\":\"DARK\"")
                .contains("\"primaryColor\":\"#FF0000\"")
                .contains("\"secondaryColor\":\"#00FF00\"");
        assertThat(response.primaryColor()).isEqualTo("#FF0000");
    }

    @Test
    void updateTheme_ownerAndSuperAdmin_shareOnePersistedSetting() {
        ownerSettingService.updateTheme(new UpdateThemeRequest("DARK", "#123456", "#654321"));
        String afterOwner = setting.getSettings();

        authenticateSuperAdmin();
        ThemeSettingsResponse asSuperAdmin = ownerSettingService.getTheme();
        ownerSettingService.updateTheme(new UpdateThemeRequest("LIGHT", "#AABBCC", "#DDEEFF"));
        String afterSuperAdmin = setting.getSettings();

        // The super admin reads and writes the very row the owner wrote.
        assertThat(asSuperAdmin.primaryColor()).isEqualTo("#123456");
        assertThat(afterOwner).isNotEqualTo(afterSuperAdmin);
        verify(ownerSettingRepository, org.mockito.Mockito.atLeast(2))
                .findByOwnerId(5L);
        verify(ownerSettingRepository, never()).findByOwnerId(1L);
    }

    @Test
    void resetTheme_bySuperAdmin_updatesSharedOwnerSetting() {
        authenticateSuperAdmin();

        ThemeSettingsResponse response = ownerSettingService.resetTheme();

        verify(ownerSettingRepository).findByOwnerId(5L);
        verify(ownerSettingRepository).save(setting);
        assertThat(response.primaryColor()).isEqualTo("#2563EB");
        assertThat(response.secondaryColor()).isEqualTo("#0F172A");
    }

    @Test
    void updateTheme_byManager_isForbidden() {
        authenticateRole(SecurityUtils.ROLE_MANAGER);

        assertThatThrownBy(() -> ownerSettingService.updateTheme(
                new UpdateThemeRequest("DARK", "#ff0000", "#00ff00")))
                .isInstanceOf(AccessDeniedException.class);

        verify(ownerSettingRepository, never()).save(any());
    }

    @Test
    void updateTheme_byTenant_isForbidden() {
        authenticateRole(SecurityUtils.ROLE_TENANT);

        assertThatThrownBy(() -> ownerSettingService.updateTheme(
                new UpdateThemeRequest("DARK", "#ff0000", "#00ff00")))
                .isInstanceOf(AccessDeniedException.class);

        verify(ownerSettingRepository, never()).save(any());
    }

    private void authenticateSuperAdmin() {
        User admin = User.builder().id(1L).username("admin1").email("admin@test.com").build();
        when(securityUtils.getCurrentUser()).thenReturn(admin);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
    }

    private void authenticateRole(String roleCode) {
        User user = User.builder().id(9L).username("user9").email("user9@test.com").build();
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(securityUtils.hasRole(roleCode)).thenReturn(true);
    }

    @Test
    void resetTheme_resetsToDefaultsAndAudits() {
        ThemeSettingsResponse response = ownerSettingService.resetTheme();

        verify(ownerSettingRepository).save(setting);
        assertThat(response.themeMode()).isEqualTo("LIGHT");
        assertThat(response.primaryColor()).isEqualTo("#2563EB");
        assertThat(response.secondaryColor()).isEqualTo("#0F172A");
        verify(securityAuditLogger).log(eq(5L), eq("THEME_RESET"), any());
    }

    @Test
    void resetTheme_byNonOwner_isForbidden() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        assertThatThrownBy(() -> ownerSettingService.resetTheme())
                .isInstanceOf(AccessDeniedException.class);

        verify(ownerSettingRepository, never()).save(any());
    }

    @Test
    void updateSettings_preservesExistingThemeBlock() {
        updateThemeSettings();

        assertThat(setting.getSettings())
                .contains("\"themeMode\":\"DARK\"")
                .contains("\"primaryColor\":\"#FF0000\"")
                .contains("exchangeRate\":4100");
    }

    private void updateThemeSettings() {
        UpdateThemeRequest themeUpdate = new UpdateThemeRequest("DARK", "#ff0000", "#00ff00");
        ownerSettingService.updateTheme(themeUpdate);
        ownerSettingService.updateSettings(fullSettingsRequest());
    }

    private UpdateOwnerSettingsRequest fullSettingsRequest() {
        return new UpdateOwnerSettingsRequest(
                "USD", "USD", new BigDecimal("4100"), "DD/MM/YYYY", "Asia/Phnom_Penh",
                "Business", "", "", "", "", "#3b82f6", "#10b981",
                "KM",
                "INV", 10, "", true, true, true, true,
                "កិច្ចសន្យាជួលផ្ទះ", "", "", "", "", "KM", null,
                "RCP", "KM", "", true, true, true, true,
                "", "");
    }

    // ---- Contract Party A: the owner's own identity on rental agreements ----

    private UpdateOwnerContractInfoRequest contractParty(String fullName, Gender gender,
            LocalDate dob, String nationality, String nationalId, String address,
            String phone, String occupation) {
        return new UpdateOwnerContractInfoRequest(
                fullName, gender, dob, nationality, nationalId, address, phone, occupation);
    }

    private UpdateOwnerSettingsRequest settingsWithParty(UpdateOwnerContractInfoRequest party) {
        return new UpdateOwnerSettingsRequest(
                "USD", "USD", new BigDecimal("4100"), "DD/MM/YYYY", "Asia/Phnom_Penh",
                "Business", "", "", "", "", "#3b82f6", "#10b981",
                "KM",
                "INV", 10, "", true, true, true, true,
                "កិច្ចសន្យាជួលផ្ទះ", "", "", "", "", "KM", party,
                "RCP", "KM", "", true, true, true, true,
                "", "");
    }

    @Test
    void getSettings_returnsOwnConfiguredContractParty() {
        ownerSettingService.updateSettings(settingsWithParty(contractParty(
                "  សុភា សុខាជ  ", Gender.FEMALE, LocalDate.of(1990, 5, 20),
                " ខ្មែរ ", "  N012345678 ", "  ភ្នំពេញ  ", " 012111222 ", " អាជីវករ ")));

        OwnerSettingsResponse response = ownerSettingService.getSettings();

        OwnerContractInfoResponse party = response.contractParty();
        assertThat(party.fullName()).isEqualTo("សុភា សុខាជ");
        assertThat(party.gender()).isEqualTo(Gender.FEMALE);
        assertThat(party.dateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(party.nationality()).isEqualTo("ខ្មែរ");
        assertThat(party.nationalId()).isEqualTo("N012345678");
        assertThat(party.currentAddress()).isEqualTo("ភ្នំពេញ");
        assertThat(party.phone()).isEqualTo("012111222");
        assertThat(party.occupation()).isEqualTo("អាជីវករ");
    }

    @Test
    void updateSettings_persistsTrimmedContractPartyAndReturnsIt() {
        OwnerSettingsResponse response = ownerSettingService.updateSettings(settingsWithParty(
                contractParty("  Sokha  ", Gender.MALE, null, "  ខ្មែរ  ", "  ID-1  ",
                        "  Street 1  ", "  012999888  ", "  គ្រូ  ")));

        assertThat(response.contractParty().fullName()).isEqualTo("Sokha");
        assertThat(response.contractParty().nationalId()).isEqualTo("ID-1");
        assertThat(response.contractParty().phone()).isEqualTo("012999888");
        assertThat(response.contractParty().occupation()).isEqualTo("គ្រូ");
        assertThat(setting.getSettings())
                .contains("\"contractParty\"")
                .contains("\"fullName\":\"Sokha\"")
                .contains("\"phone\":\"012999888\"")
                .contains("\"gender\":\"MALE\"");
    }

    @Test
    void updateSettings_allowsOptionalContractPartyFieldsToStayEmpty() {
        OwnerSettingsResponse response = ownerSettingService.updateSettings(settingsWithParty(
                contractParty(null, null, null, "  ", "", null, "  ", "")));

        OwnerContractInfoResponse party = response.contractParty();
        assertThat(party.fullName()).isEmpty();
        assertThat(party.gender()).isNull();
        assertThat(party.dateOfBirth()).isNull();
        assertThat(party.nationality()).isEmpty();
        assertThat(party.nationalId()).isEmpty();
        assertThat(party.currentAddress()).isEmpty();
        assertThat(party.phone()).isEmpty();
        assertThat(party.occupation()).isEmpty();
    }

    @Test
    void updateSettings_preservesContractPartyWhenRequestOmitsIt() {
        ownerSettingService.updateSettings(settingsWithParty(contractParty(
                "Sokha", Gender.MALE, null, "ខ្មែរ", "ID-1", "Street 1", "012111222", "គ្រូ")));

        ownerSettingService.updateSettings(fullSettingsRequest());

        OwnerContractInfoResponse party = ownerSettingService.getSettings().contractParty();
        assertThat(party.fullName()).isEqualTo("Sokha");
        assertThat(party.phone()).isEqualTo("012111222");
        assertThat(party.nationalId()).isEqualTo("ID-1");
    }

    @Test
    void getSettings_byNonOwner_isForbidden() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        assertThatThrownBy(() -> ownerSettingService.getSettings())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateSettings_byNonOwner_isForbiddenAndSavesNothing() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        assertThatThrownBy(() -> ownerSettingService.updateSettings(
                settingsWithParty(contractParty("Intruder", null, null, null, null, null, null, null))))
                .isInstanceOf(AccessDeniedException.class);

        verify(ownerSettingRepository, never()).save(any());
    }

    @Test
    void contractParty_isAlwaysReadFromTheAuthenticatedOwnersOwnRow() {
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        ownerSettingService.updateSettings(settingsWithParty(contractParty(
                "Sokha", null, null, null, null, null, "012111222", null)));

        ownerSettingService.getSettings();

        // The request carries no owner id, so there is no way to address another
        // owner's row: only the authenticated owner's id is ever looked up.
        verify(ownerSettingRepository, never()).findByOwnerId(999L);
        verify(ownerSettingRepository, atLeastOnce()).findByOwnerId(5L);
    }

    @Test
    void getSettings_seedsContractPartyFromOwnerAccountOnFirstLoad() {
        User newOwner = User.builder().id(7L).username("owner7").email("o7@test.com")
                .firstName("Dara").lastName("Sok").phone("011222333").build();
        when(securityUtils.getCurrentUser()).thenReturn(newOwner);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(ownerSettingRepository.findByOwnerId(7L)).thenReturn(Optional.empty());
        when(ownerSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OwnerSettingsResponse response = ownerSettingService.getSettings();

        assertThat(response.contractParty().fullName()).isEqualTo("Dara Sok");
        assertThat(response.contractParty().phone()).isEqualTo("011222333");
        assertThat(response.contractParty().gender()).isNull();
    }

    // ---- Accessor consumed by rental-contract creation ----

    @Test
    void getContractInfo_returnsConfiguredInfoForTheGivenOwner() {
        ownerSettingService.updateSettings(settingsWithParty(contractParty(
                "Sokha", Gender.MALE, LocalDate.of(1985, 1, 2), "ខ្មែរ", "ID-9",
                "Street 9", "012111222", "គ្រូ")));

        OwnerContractInfoResponse info = ownerSettingService.getContractInfo(5L);

        assertThat(info.fullName()).isEqualTo("Sokha");
        assertThat(info.gender()).isEqualTo(Gender.MALE);
        assertThat(info.dateOfBirth()).isEqualTo(LocalDate.of(1985, 1, 2));
        assertThat(info.nationality()).isEqualTo("ខ្មែរ");
        assertThat(info.nationalId()).isEqualTo("ID-9");
        assertThat(info.currentAddress()).isEqualTo("Street 9");
        assertThat(info.phone()).isEqualTo("012111222");
        assertThat(info.occupation()).isEqualTo("គ្រូ");
    }

    @Test
    void getContractInfo_returnsBlanks_whenOwnerHasNoSettingsRow() {
        when(ownerSettingRepository.findByOwnerId(5L)).thenReturn(Optional.empty());

        OwnerContractInfoResponse info = ownerSettingService.getContractInfo(5L);

        assertThat(info.fullName()).isEmpty();
        assertThat(info.gender()).isNull();
        assertThat(info.dateOfBirth()).isNull();
        assertThat(info.phone()).isEmpty();
    }

    @Test
    void getContractInfo_returnsBlanks_whenOwnerIdIsNull() {
        OwnerContractInfoResponse info = ownerSettingService.getContractInfo(null);

        assertThat(info.fullName()).isEmpty();
        verify(ownerSettingRepository, never()).findByOwnerId(any());
    }

    @Test
    void getContractInfo_toleratesSettingsDocumentWithoutContractPartyBlock() {
        when(ownerSettingRepository.findByOwnerId(5L)).thenReturn(Optional.of(
                OwnerSetting.builder().id(1L).owner(owner)
                        .settings("{\"baseCurrency\":\"USD\"}").build()));

        OwnerContractInfoResponse info = ownerSettingService.getContractInfo(5L);

        assertThat(info.fullName()).isEmpty();
        assertThat(info.occupation()).isEmpty();
    }
}
