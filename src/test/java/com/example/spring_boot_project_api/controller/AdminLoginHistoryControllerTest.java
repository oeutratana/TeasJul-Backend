package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.LoginHistoryResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.enums.LoginMethod;
import com.example.spring_boot_project_api.enums.LoginOutcome;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.service.LoginHistoryService;
import com.example.spring_boot_project_api.util.JwtUtil;

import org.assertj.core.api.Assertions;

/**
 * The role check itself lives in {@code LoginHistoryServiceImpl} (covered by
 * {@code LoginHistoryServiceImplTest}); this slice verifies routing, pagination
 * wiring and that the endpoint requires authentication.
 */
@WebMvcTest(AdminLoginHistoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class AdminLoginHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginHistoryService loginHistoryService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void shouldReturnTheRequestedUsersHistory() throws Exception {
        when(loginHistoryService.getHistoryForUser(eq(42L), any())).thenReturn(PageResponse.of(
                List.of(new LoginHistoryResponse(9L, LocalDateTime.of(2026, 9, 25, 22, 30),
                        LoginOutcome.SUCCESS, LoginMethod.PASSWORD_2FA, "203.0.113.7",
                        "Mozilla/5.0 Chrome/140.0")),
                0, 10, 1, 1));

        mockMvc.perform(get("/api/admin/users/42/login-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].method").value("PASSWORD_2FA"))
                .andExpect(jsonPath("$.content[0].ip").value("203.0.113.7"));
    }

    @Test
    @WithMockUser
    void shouldHonourPageAndSize() throws Exception {
        when(loginHistoryService.getHistoryForUser(any(), any())).thenReturn(PageResponse.of(
                List.of(), 2, 20, 0, 0));

        mockMvc.perform(get("/api/admin/users/42/login-history")
                        .param("page", "2").param("size", "20"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(loginHistoryService).getHistoryForUser(eq(42L), captor.capture());
        Assertions.assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        Assertions.assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @WithMockUser
    void shouldSurfaceAnUnknownAccountAsNotFound() throws Exception {
        when(loginHistoryService.getHistoryForUser(eq(404L), any()))
                .thenThrow(UserNotFoundException.forId(404L));

        mockMvc.perform(get("/api/admin/users/404/login-history"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldSurfaceAnAccessDeniedAsForbidden() throws Exception {
        when(loginHistoryService.getHistoryForUser(eq(42L), any()))
                .thenThrow(new AccessDeniedException("You do not have permission to access this resource"));

        mockMvc.perform(get("/api/admin/users/42/login-history"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/users/42/login-history"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(loginHistoryService);
    }
}
