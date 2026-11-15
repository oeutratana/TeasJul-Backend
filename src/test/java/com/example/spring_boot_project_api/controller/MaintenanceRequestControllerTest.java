package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
import com.example.spring_boot_project_api.dto.response.MaintenanceRequestResponse;
import com.example.spring_boot_project_api.model.MaintenanceRequestPriority;
import com.example.spring_boot_project_api.model.MaintenanceRequestStatus;
import com.example.spring_boot_project_api.service.MaintenanceRequestService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(MaintenanceRequestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class MaintenanceRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaintenanceRequestService requestService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private MaintenanceRequestResponse sampleResponse() {
        return new MaintenanceRequestResponse(80L, 1L, "MR-20260901-0004",
                8L, 12L, 50L, null, "Leaking pipe", "Kitchen sink leaks",
                MaintenanceRequestPriority.URGENT, MaintenanceRequestStatus.PENDING,
                null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void myRequestsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/maintenance-requests/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyRequestsAsTenantReturnsTenantRequests() throws Exception {
        when(requestService.getMyRequestsAsTenant()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/maintenance-requests/my").with(user("sopha").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantId").value(50))
                .andExpect(jsonPath("$[0].title").value("Leaking pipe"));
    }

    @Test
    void createMyRequestReturnsCreatedRequest() throws Exception {
        when(requestService.createMyRequest(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/maintenance-requests/my")
                        .with(user("sopha").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Leaking pipe",
                                  "description": "Kitchen sink leaks",
                                  "priority": "URGENT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Leaking pipe"))
                .andExpect(jsonPath("$.priority").value("URGENT"));
    }

    @Test
    void createMyRequestRequiresTitle() throws Exception {
        mockMvc.perform(post("/api/v1/maintenance-requests/my")
                        .with(user("sopha").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Kitchen sink leaks"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}