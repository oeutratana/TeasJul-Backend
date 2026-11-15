package com.example.spring_boot_project_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.LoginHistoryResponse;
import com.example.spring_boot_project_api.dto.response.LoginResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.enums.LoginMethod;
import com.example.spring_boot_project_api.enums.LoginOutcome;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.service.AuthService;
import com.example.spring_boot_project_api.service.LoginHistoryService;
import com.example.spring_boot_project_api.service.SecurityService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SecurityService securityService;

    @MockitoBean
    private LoginHistoryService loginHistoryService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserResponse sampleUser() {
        return new UserResponse(1L, "johndoe", "john@example.com", "John", "Doe",
                "+1234567890", null, UserStatus.ACTIVE, null, null, null);
    }

    @Test
    void registerShouldReturnCreatedUser() throws Exception {
        when(authService.register(any())).thenReturn(sampleUser());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "johndoe",
                                  "email": "john@example.com",
                                  "password": "Password123",
                                  "firstName": "John",
                                  "lastName": "Doe"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerShouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "email": "not-an-email",
                                  "password": "short",
                                  "firstName": "",
                                  "lastName": "Doe"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void registerShouldReturnConflictForDuplicateUsername() throws Exception {
        when(authService.register(any()))
                .thenThrow(new DuplicateResourceException("Username is already taken"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "johndoe",
                                  "email": "john@example.com",
                                  "password": "Password123",
                                  "firstName": "John",
                                  "lastName": "Doe"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username is already taken"));
    }

    @Test
    void registerShouldReturnConflictForDuplicateEmail() throws Exception {
        when(authService.register(any()))
                .thenThrow(new DuplicateResourceException("Email is already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "newuser",
                                  "email": "john@example.com",
                                  "password": "Password123",
                                  "firstName": "John",
                                  "lastName": "Doe"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void loginShouldReturnToken() throws Exception {
        when(authService.login(any(), any())).thenReturn(
                new LoginResponse("jwt-token-value", "Bearer", 900000L, sampleUser()));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "johndoe",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900000))
                .andExpect(jsonPath("$.user.username").value("johndoe"));
    }

    @Test
    void loginShouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        when(authService.login(any(), any()))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "johndoe",
                                  "password": "WrongPassword1"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    private PageResponse<LoginHistoryResponse> historyPage() {
        return PageResponse.of(List.of(
                new LoginHistoryResponse(2L, LocalDateTime.of(2026, 9, 25, 22, 30),
                        LoginOutcome.SUCCESS, LoginMethod.PASSWORD, "192.168.1.10",
                        "Mozilla/5.0 Chrome/140.0"),
                new LoginHistoryResponse(1L, LocalDateTime.of(2026, 9, 25, 22, 25),
                        LoginOutcome.FAILED, LoginMethod.PASSWORD, "192.168.1.11",
                        "Mozilla/5.0 Firefox/141.0")),
                0, 10, 2, 1);
    }

    @Test
    @WithMockUser
    void loginHistoryShouldReturnTheCallersOwnRecords() throws Exception {
        when(loginHistoryService.getMyHistory(any())).thenReturn(historyPage());

        mockMvc.perform(get("/api/v1/auth/login-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].method").value("PASSWORD"))
                .andExpect(jsonPath("$.content[0].ip").value("192.168.1.10"))
                .andExpect(jsonPath("$.content[0].userAgent").value("Mozilla/5.0 Chrome/140.0"))
                .andExpect(jsonPath("$.content[1].outcome").value("FAILED"))
                .andExpect(jsonPath("$.content[1].method").value("PASSWORD"));
    }

    @Test
    @WithMockUser
    void loginHistoryShouldHonourPageAndSize() throws Exception {
        when(loginHistoryService.getMyHistory(any())).thenReturn(historyPage());

        mockMvc.perform(get("/api/v1/auth/login-history").param("page", "1").param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(loginHistoryService).getMyHistory(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("occurredAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void loginHistoryShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login-history"))
                .andExpect(status().isUnauthorized());
    }
}
