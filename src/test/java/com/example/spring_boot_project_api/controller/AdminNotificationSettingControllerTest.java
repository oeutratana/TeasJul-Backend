package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.example.spring_boot_project_api.dto.request.UpdateSystemNotificationSettingsRequest;
import com.example.spring_boot_project_api.dto.response.SystemNotificationSettingsResponse;
import com.example.spring_boot_project_api.service.SystemSettingService;
import com.example.spring_boot_project_api.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AdminNotificationSettingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class AdminNotificationSettingControllerTest {

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
    void getNotificationSettings_returnsSuccess() throws Exception {
        when(userDetailsService.loadUserByUsername("superadmin")).thenReturn(springUser());
        SystemNotificationSettingsResponse response = new SystemNotificationSettingsResponse(
                true, true, true, true, true, true, true, true, true, true, true, true, LocalDateTime.now()
        );
        when(systemSettingService.getNotificationSettings()).thenReturn(response);

        mockMvc.perform(get("/api/admin/settings/notifications")
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemNotificationsEnabled").value(true))
                .andExpect(jsonPath("$.inAppNotificationsEnabled").value(true))
                .andExpect(jsonPath("$.telegramNotificationsEnabled").value(true))
                .andExpect(jsonPath("$.propertySubmittedEnabled").value(true));
    }

    @Test
    void updateNotificationSettings_returnsUpdatedResponse() throws Exception {
        when(userDetailsService.loadUserByUsername("superadmin")).thenReturn(springUser());
        SystemNotificationSettingsResponse response = new SystemNotificationSettingsResponse(
                true, true, false, false, true, true, true, true, true, true, true, true, LocalDateTime.now()
        );
        when(systemSettingService.updateNotificationSettings(any(UpdateSystemNotificationSettingsRequest.class)))
                .thenReturn(response);

        UpdateSystemNotificationSettingsRequest request = new UpdateSystemNotificationSettingsRequest(
                true, true, false, false, true, true, true, true, true, true, true, true
        );

        mockMvc.perform(put("/api/admin/settings/notifications")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramNotificationsEnabled").value(false))
                .andExpect(jsonPath("$.propertySubmittedEnabled").value(false));
    }
}
