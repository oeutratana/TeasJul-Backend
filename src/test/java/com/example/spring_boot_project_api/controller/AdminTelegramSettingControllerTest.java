package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.SuperAdminTelegramSettingsResponse;
import com.example.spring_boot_project_api.service.SystemSettingService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(AdminTelegramSettingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class AdminTelegramSettingControllerTest {

    private static final String REAL_TOKEN = "123456789:AAFakeTokenForTestsOnly_doNotUse";
    private static final String MASK = "***************";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private SystemSettingService systemSettingService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private String tokenFor(String username, String authority) {
        UserDetails user = User.builder()
                .username(username)
                .password("hash")
                .authorities(authority)
                .build();
        when(userDetailsService.loadUserByUsername(username)).thenReturn(user);
        return jwtUtil.generateToken(username);
    }

    private String superAdminToken() {
        return tokenFor("superadmin", "ROLE_SUPER_ADMIN");
    }

    private SuperAdminTelegramSettingsResponse settings() {
        return new SuperAdminTelegramSettingsResponse(true, true, MASK, "-1001234567890",
                LocalDateTime.now());
    }

    @Test
    void getTelegramSettings_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/settings/telegram"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTelegramSettings_returnsMaskedTokenForSuperAdmin() throws Exception {
        when(systemSettingService.getSuperAdminTelegramSettings()).thenReturn(settings());

        mockMvc.perform(get("/api/admin/settings/telegram")
                        .header("Authorization", "Bearer " + superAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.botTokenConfigured").value(true))
                .andExpect(jsonPath("$.maskedBotToken").value(MASK))
                .andExpect(jsonPath("$.chatId").value("-1001234567890"))
                .andExpect(jsonPath("$.botToken").doesNotExist());
    }

    @Test
    void getTelegramSettings_neverLeaksTheStoredToken() throws Exception {
        when(systemSettingService.getSuperAdminTelegramSettings())
                .thenReturn(new SuperAdminTelegramSettingsResponse(true, true, MASK, "-1001234567890",
                        LocalDateTime.now()));

        String body = mockMvc.perform(get("/api/admin/settings/telegram")
                        .header("Authorization", "Bearer " + superAdminToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body).doesNotContain(REAL_TOKEN);
    }

    @Test
    void updateTelegramSettings_succeedsForSuperAdmin() throws Exception {
        when(systemSettingService.updateSuperAdminTelegramSettings(any())).thenReturn(settings());

        mockMvc.perform(put("/api/admin/settings/telegram")
                        .header("Authorization", "Bearer " + superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"botToken\":\"" + REAL_TOKEN + "\",\"chatId\":\"-1001234567890\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botTokenConfigured").value(true))
                .andExpect(jsonPath("$.botToken").doesNotExist());

        verify(systemSettingService).updateSuperAdminTelegramSettings(any());
    }

    @Test
    void updateTelegramSettings_keepsTokenWhenMaskIsSentBack() throws Exception {
        when(systemSettingService.updateSuperAdminTelegramSettings(any())).thenReturn(settings());

        mockMvc.perform(put("/api/admin/settings/telegram")
                        .header("Authorization", "Bearer " + superAdminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"botToken\":\"" + MASK + "\",\"chatId\":\"-1001234567890\"}"))
                .andExpect(status().isOk());

        verify(systemSettingService).updateSuperAdminTelegramSettings(any());
    }

    @Test
    void updateTelegramSettings_forbiddenForOwner() throws Exception {
        when(systemSettingService.updateSuperAdminTelegramSettings(any()))
                .thenThrow(new AccessDeniedException("Only SUPER_ADMIN can manage the super admin Telegram settings"));

        mockMvc.perform(put("/api/admin/settings/telegram")
                        .header("Authorization", "Bearer " + tokenFor("owner1", "ROLE_OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"botToken\":\"" + REAL_TOKEN + "\",\"chatId\":\"-1001234567890\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTelegramSettings_forbiddenForManager() throws Exception {
        when(systemSettingService.updateSuperAdminTelegramSettings(any()))
                .thenThrow(new AccessDeniedException("Only SUPER_ADMIN can manage the super admin Telegram settings"));

        mockMvc.perform(put("/api/admin/settings/telegram")
                        .header("Authorization", "Bearer " + tokenFor("manager1", "ROLE_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"botToken\":\"" + REAL_TOKEN + "\",\"chatId\":\"-1001234567890\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTelegramSettings_forbiddenForTenant() throws Exception {
        when(systemSettingService.updateSuperAdminTelegramSettings(any()))
                .thenThrow(new AccessDeniedException("Only SUPER_ADMIN can manage the super admin Telegram settings"));

        mockMvc.perform(put("/api/admin/settings/telegram")
                        .header("Authorization", "Bearer " + tokenFor("tenant1", "ROLE_TENANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"botToken\":\"" + REAL_TOKEN + "\",\"chatId\":\"-1001234567890\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTelegramSettings_forbiddenForOwner() throws Exception {
        when(systemSettingService.getSuperAdminTelegramSettings())
                .thenThrow(new AccessDeniedException("Only SUPER_ADMIN can manage the super admin Telegram settings"));

        mockMvc.perform(get("/api/admin/settings/telegram")
                        .header("Authorization", "Bearer " + tokenFor("owner1", "ROLE_OWNER")))
                .andExpect(status().isForbidden());
    }
}
