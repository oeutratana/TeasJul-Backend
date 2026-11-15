package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.request.UpdateLoginRateLimitSettingsRequest;
import com.example.spring_boot_project_api.dto.response.LoginRateLimitSettingsResponse;
import com.example.spring_boot_project_api.service.SystemSettingService;
import com.example.spring_boot_project_api.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AdminLoginRateLimitSettingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class AdminLoginRateLimitSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private SystemSettingService systemSettingService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private UserDetails springUser() {
        return User.builder()
                .username("superadmin")
                .password("hash")
                .authorities("ROLE_SUPER_ADMIN")
                .build();
    }

    private String token() {
        return jwtUtil.generateToken("superadmin");
    }

    @Test
    void getLoginRateLimitSettings_returnsSuccess() throws Exception {
        when(userDetailsService.loadUserByUsername("superadmin")).thenReturn(springUser());
        when(systemSettingService.getLoginRateLimitSettings())
                .thenReturn(new LoginRateLimitSettingsResponse(true, 5, 15, LocalDateTime.now()));

        mockMvc.perform(get("/api/admin/settings/security/login-rate-limit")
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginRateLimitEnabled").value(true))
                .andExpect(jsonPath("$.maxFailedAttempts").value(5))
                .andExpect(jsonPath("$.lockoutDurationMinutes").value(15));
    }

    @Test
    void updateLoginRateLimitSettings_returnsUpdatedResponse() throws Exception {
        when(userDetailsService.loadUserByUsername("superadmin")).thenReturn(springUser());
        when(systemSettingService.updateLoginRateLimitSettings(any(UpdateLoginRateLimitSettingsRequest.class)))
                .thenReturn(new LoginRateLimitSettingsResponse(false, 9, 45, LocalDateTime.now()));

        mockMvc.perform(put("/api/admin/settings/security/login-rate-limit")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginRateLimitEnabled\":false,\"maxFailedAttempts\":9,\"lockoutDurationMinutes\":45}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginRateLimitEnabled").value(false))
                .andExpect(jsonPath("$.maxFailedAttempts").value(9))
                .andExpect(jsonPath("$.lockoutDurationMinutes").value(45));
    }

    @Test
    void getLoginRateLimitSettings_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/settings/security/login-rate-limit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateLoginRateLimitSettings_requiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/admin/settings/security/login-rate-limit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginRateLimitEnabled\":true,\"maxFailedAttempts\":5,\"lockoutDurationMinutes\":15}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateLoginRateLimitSettings_rejectsZeroAttempts() throws Exception {
        when(userDetailsService.loadUserByUsername("superadmin")).thenReturn(springUser());

        mockMvc.perform(put("/api/admin/settings/security/login-rate-limit")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginRateLimitEnabled\":true,\"maxFailedAttempts\":0,\"lockoutDurationMinutes\":15}"))
                .andExpect(status().isBadRequest());

        verify(systemSettingService, never())
                .updateLoginRateLimitSettings(any(UpdateLoginRateLimitSettingsRequest.class));
    }

    @Test
    void updateLoginRateLimitSettings_rejectsNegativeLockout() throws Exception {
        when(userDetailsService.loadUserByUsername("superadmin")).thenReturn(springUser());

        mockMvc.perform(put("/api/admin/settings/security/login-rate-limit")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginRateLimitEnabled\":true,\"maxFailedAttempts\":5,\"lockoutDurationMinutes\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLoginRateLimitSettings_rejectsExcessiveValues() throws Exception {
        when(userDetailsService.loadUserByUsername("superadmin")).thenReturn(springUser());

        mockMvc.perform(put("/api/admin/settings/security/login-rate-limit")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginRateLimitEnabled\":true,\"maxFailedAttempts\":5000,\"lockoutDurationMinutes\":15}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLoginRateLimitSettings_rejectsFractionalAttempts() throws Exception {
        when(userDetailsService.loadUserByUsername("superadmin")).thenReturn(springUser());

        mockMvc.perform(put("/api/admin/settings/security/login-rate-limit")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginRateLimitEnabled\":true,\"maxFailedAttempts\":5.7,\"lockoutDurationMinutes\":15}"))
                .andExpect(status().isBadRequest());

        verify(systemSettingService, never())
                .updateLoginRateLimitSettings(any(UpdateLoginRateLimitSettingsRequest.class));
    }

    @Test
    void updateLoginRateLimitSettings_rejectsNonBooleanEnabledFlag() throws Exception {
        when(userDetailsService.loadUserByUsername("superadmin")).thenReturn(springUser());

        mockMvc.perform(put("/api/admin/settings/security/login-rate-limit")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginRateLimitEnabled\":\"yes\",\"maxFailedAttempts\":5,\"lockoutDurationMinutes\":15}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLoginRateLimitSettings_rejectsMissingFields() throws Exception {
        when(userDetailsService.loadUserByUsername("superadmin")).thenReturn(springUser());

        mockMvc.perform(put("/api/admin/settings/security/login-rate-limit")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginRateLimitEnabled\":true}"))
                .andExpect(status().isBadRequest());
    }
}
