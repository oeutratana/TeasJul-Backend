package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.UserSecurityResponse;
import com.example.spring_boot_project_api.service.SecurityService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(SecurityController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class SecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecurityService securityService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void getSecurityShouldReturnCurrentSettings() throws Exception {
        when(securityService.getMySecurity())
                .thenReturn(new UserSecurityResponse(
                        "john.doe@gmail.com", true, null, true, "EMAIL"));

        mockMvc.perform(get("/api/v1/security"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryEmail").value("john.doe@gmail.com"))
                .andExpect(jsonPath("$.twoFactorEnabled").value(true));
    }

    @Test
    @WithMockUser
    void addRecoveryEmailShouldReturnMessage() throws Exception {
        when(securityService.addRecoveryEmail(any(), any()))
                .thenReturn(com.example.spring_boot_project_api.dto.response.MessageResponse
                        .of("A verification code has been sent to the provided email address."));

        mockMvc.perform(post("/api/v1/security/recovery-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recoveryEmail\":\"john.doe@gmail.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    void verifyRecoveryEmailShouldReturnMessage() throws Exception {
        when(securityService.verifyRecoveryEmail(any(), any()))
                .thenReturn(com.example.spring_boot_project_api.dto.response.MessageResponse
                        .of("Your recovery email has been verified."));

        mockMvc.perform(post("/api/v1/security/recovery-email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recoveryEmail\":\"john.doe@gmail.com\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your recovery email has been verified."));
    }

    @Test
    @WithMockUser
    void enableTwoFactorShouldReturnMessage() throws Exception {
        when(securityService.enableTwoFactor(any()))
                .thenReturn(com.example.spring_boot_project_api.dto.response.MessageResponse
                        .of("Two-factor authentication enabled."));

        mockMvc.perform(post("/api/v1/security/2fa/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Two-factor authentication enabled."));
    }

    @Test
    @WithMockUser
    void disableTwoFactorShouldReturnMessage() throws Exception {
        when(securityService.disableTwoFactor(any()))
                .thenReturn(com.example.spring_boot_project_api.dto.response.MessageResponse
                        .of("Two-factor authentication disabled."));

        mockMvc.perform(post("/api/v1/security/2fa/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Two-factor authentication disabled."));
    }
}
