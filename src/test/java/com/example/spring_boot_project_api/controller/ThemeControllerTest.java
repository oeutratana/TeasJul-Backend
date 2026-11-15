package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.ThemeSettingsResponse;
import com.example.spring_boot_project_api.service.OwnerSettingService;
import com.example.spring_boot_project_api.util.JwtUtil;
import com.example.spring_boot_project_api.util.SecurityUtils;

@WebMvcTest(ThemeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class ThemeControllerTest {

    private static final String VALID_THEME =
            "{\"themeMode\":\"DARK\",\"primaryColor\":\"#2563EB\",\"secondaryColor\":\"#0F172A\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerSettingService ownerSettingService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
    }

    private ThemeSettingsResponse sampleTheme() {
        return new ThemeSettingsResponse(5L, "DARK", "#2563EB", "#0F172A");
    }

    @Test
    void getTheme_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/settings/theme"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTheme_returnsOrganizationThemeForOwner() throws Exception {
        when(ownerSettingService.getTheme()).thenReturn(sampleTheme());

        mockMvc.perform(get("/api/v1/settings/theme").with(user("sok").roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(5))
                .andExpect(jsonPath("$.themeMode").value("DARK"))
                .andExpect(jsonPath("$.primaryColor").value("#2563EB"))
                .andExpect(jsonPath("$.secondaryColor").value("#0F172A"));
    }

    @Test
    void getTheme_availableToManager() throws Exception {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(ownerSettingService.getTheme()).thenReturn(sampleTheme());

        mockMvc.perform(get("/api/v1/settings/theme").with(user("sopha").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeMode").value("DARK"));
    }

    @Test
    void updateTheme_byOwner_succeeds() throws Exception {
        when(ownerSettingService.updateTheme(any())).thenReturn(sampleTheme());

        mockMvc.perform(put("/api/v1/settings/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_THEME)
                        .with(user("sok").roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeMode").value("DARK"));

        verify(ownerSettingService).updateTheme(any());
    }

    @Test
    void updateTheme_byManager_isForbidden() throws Exception {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        mockMvc.perform(put("/api/v1/settings/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_THEME)
                        .with(user("sopha").roles("MANAGER")))
                .andExpect(status().isForbidden());

        verify(ownerSettingService, never()).updateTheme(any());
    }

    @Test
    void updateTheme_byTenant_isForbidden() throws Exception {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        mockMvc.perform(put("/api/v1/settings/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_THEME)
                        .with(user("dara").roles("TENANT")))
                .andExpect(status().isForbidden());

        verify(ownerSettingService, never()).updateTheme(any());
    }

    @Test
    void updateTheme_rejectsInvalidColor() throws Exception {
        mockMvc.perform(put("/api/v1/settings/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"themeMode\":\"DARK\",\"primaryColor\":\"blue\",\"secondaryColor\":\"#0F172A\"}")
                        .with(user("sok").roles("OWNER")))
                .andExpect(status().isBadRequest());

        verify(ownerSettingService, never()).updateTheme(any());
    }

    @Test
    void updateTheme_rejectsInvalidThemeMode() throws Exception {
        mockMvc.perform(put("/api/v1/settings/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"themeMode\":\"FLORESCENT\",\"primaryColor\":\"#2563EB\",\"secondaryColor\":\"#0F172A\"}")
                        .with(user("sok").roles("OWNER")))
                .andExpect(status().isBadRequest());

        verify(ownerSettingService, never()).updateTheme(any());
    }

    @Test
    void updateTheme_rejectsMissingFields() throws Exception {
        mockMvc.perform(put("/api/v1/settings/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"themeMode\":\"LIGHT\"}")
                        .with(user("sok").roles("OWNER")))
                .andExpect(status().isBadRequest());

        verify(ownerSettingService, never()).updateTheme(any());
    }

    @Test
    void resetTheme_byOwner_succeeds() throws Exception {
        when(ownerSettingService.resetTheme()).thenReturn(
                new ThemeSettingsResponse(5L, "LIGHT", "#2563EB", "#0F172A"));

        mockMvc.perform(post("/api/v1/settings/theme/reset")
                        .with(user("sok").roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeMode").value("LIGHT"))
                .andExpect(jsonPath("$.primaryColor").value("#2563EB"));

        verify(ownerSettingService).resetTheme();
    }

    @Test
    void updateTheme_bySuperAdmin_succeeds() throws Exception {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(ownerSettingService.updateTheme(any())).thenReturn(sampleTheme());

        mockMvc.perform(put("/api/v1/settings/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_THEME)
                        .with(user("admin1").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#2563EB"));

        verify(ownerSettingService).updateTheme(any());
    }

    @Test
    void getTheme_availableToSuperAdmin() throws Exception {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(ownerSettingService.getTheme()).thenReturn(sampleTheme());

        mockMvc.perform(get("/api/v1/settings/theme").with(user("admin1").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#2563EB"));
    }

    @Test
    void updateTheme_bySuperAdmin_rejectsInvalidColor() throws Exception {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);

        mockMvc.perform(put("/api/v1/settings/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"themeMode\":\"DARK\",\"primaryColor\":\"blue\",\"secondaryColor\":\"#0F172A\"}")
                        .with(user("admin1").roles("SUPER_ADMIN")))
                .andExpect(status().isBadRequest());

        verify(ownerSettingService, never()).updateTheme(any());
    }

    @Test
    void resetTheme_bySuperAdmin_succeeds() throws Exception {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(ownerSettingService.resetTheme()).thenReturn(
                new ThemeSettingsResponse(5L, "LIGHT", "#2563EB", "#0F172A"));

        mockMvc.perform(post("/api/v1/settings/theme/reset")
                        .with(user("admin1").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#2563EB"));

        verify(ownerSettingService).resetTheme();
    }

    @Test
    void resetTheme_byManager_isForbidden() throws Exception {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        mockMvc.perform(post("/api/v1/settings/theme/reset")
                        .with(user("sopha").roles("MANAGER")))
                .andExpect(status().isForbidden());

        verify(ownerSettingService, never()).resetTheme();
    }

    @Test
    void updateTheme_whenServiceDenies_returnsForbidden() throws Exception {
        when(ownerSettingService.updateTheme(any()))
                .thenThrow(new AccessDeniedException("Only owners can manage settings"));

        mockMvc.perform(put("/api/v1/settings/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_THEME)
                        .with(user("sok").roles("OWNER")))
                .andExpect(status().isForbidden());
    }
}