package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.InvitationConsumeResponse;
import com.example.spring_boot_project_api.dto.response.InvitationValidationResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvitationCreateResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvitationResponse;
import com.example.spring_boot_project_api.model.TenantInvitationStatus;
import com.example.spring_boot_project_api.service.TenantInvitationService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(TenantInvitationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class TenantInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantInvitationService tenantInvitationService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/tenant-invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "houseId": 10
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReturnsInvitationWithRawTokenOnce() throws Exception {
        TenantInvitationCreateResponse response = new TenantInvitationCreateResponse(
                99L, "raw-token", "/tenant/invite?token=raw-token", null,
                10L, "Golden House", LocalDateTime.now().plusMinutes(5),
                TenantInvitationStatus.PENDING);
        when(tenantInvitationService.createInvitation(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/tenant-invitations")
                        .with(user("manager").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "houseId": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.token").value("raw-token"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.tokenHash").doesNotExist());
    }

    @Test
    void createRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/tenant-invitations")
                        .with(user("manager").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.houseId").exists());
    }

    @Test
    void validateIsPubliclyAccessible() throws Exception {
        when(tenantInvitationService.validateInvitation("raw-token")).thenReturn(
                new InvitationValidationResponse(true, "VALID", "vannak@example.com",
                        "Golden House", LocalDateTime.now().plusMinutes(5)));

        mockMvc.perform(get("/api/v1/tenant-invitations/validate").param("token", "raw-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.reason").value("VALID"))
                .andExpect(jsonPath("$.email").value("vannak@example.com"))
                .andExpect(jsonPath("$.houseName").value("Golden House"));
    }

    @Test
    void validateReturnsInvalidReasonWithoutLeakingToken() throws Exception {
        when(tenantInvitationService.validateInvitation("stale")).thenReturn(
                new InvitationValidationResponse(false, "INVITATION_EXPIRED", null, null, null));

        mockMvc.perform(get("/api/v1/tenant-invitations/validate").param("token", "stale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reason").value("INVITATION_EXPIRED"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void consumeIsPubliclyAccessibleAndReturnsAccepted() throws Exception {
        when(tenantInvitationService.consumeInvitation(any())).thenReturn(
                new InvitationConsumeResponse(true, 9L, "vannak", "vannak@example.com",
                        "Invitation accepted"));

        mockMvc.perform(post("/api/v1/tenant-invitations/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "raw-token",
                                  "username": "vannak",
                                  "email": "vannak@example.com",
                                  "password": "Password123",
                                  "firstName": "Vannak",
                                  "lastName": "Sok",
                                  "phone": "012345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.tenantId").value(9))
                .andExpect(jsonPath("$.username").value("vannak"));
    }

    @Test
    void listReturnsMyInvitations() throws Exception {
        when(tenantInvitationService.getMyInvitations()).thenReturn(List.of(
                new TenantInvitationResponse(5L, 10L, "Golden House", "vannak@example.com",
                        TenantInvitationStatus.PENDING, LocalDateTime.now().plusMinutes(5),
                        null, LocalDateTime.now())));

        mockMvc.perform(get("/api/v1/tenant-invitations")
                        .with(user("manager").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].token").doesNotExist());
    }

    @Test
    void revokeReturnsRevokedInvitation() throws Exception {
        when(tenantInvitationService.revokeInvitation(5L)).thenReturn(
                new TenantInvitationResponse(5L, 10L, "Golden House", "vannak@example.com",
                        TenantInvitationStatus.REVOKED, LocalDateTime.now().plusMinutes(5),
                        null, LocalDateTime.now()));

        mockMvc.perform(delete("/api/v1/tenant-invitations/5")
                        .with(user("manager").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }
}