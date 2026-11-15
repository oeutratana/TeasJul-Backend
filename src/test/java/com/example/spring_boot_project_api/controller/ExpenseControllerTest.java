package com.example.spring_boot_project_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import com.example.spring_boot_project_api.dto.request.ExpenseRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseResponse;
import com.example.spring_boot_project_api.enums.ExpenseStatus;
import com.example.spring_boot_project_api.service.ExpenseService;
import com.example.spring_boot_project_api.util.JwtUtil;

/**
 * Web-layer regression tests for the expense API (finding F-02): JSON request
 * bodies must bind through Spring MVC, validation errors must be rejected, and
 * authorization failures must surface as 403.
 */
@WebMvcTest(ExpenseController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private ExpenseService expenseService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserDetails springUser() {
        return User.builder()
                .username("owner")
                .password("n/a")
                .roles("OWNER")
                .build();
    }

    private String validToken() {
        return jwtUtil.generateToken("owner");
    }

    private void allowJwt() {
        when(userDetailsService.loadUserByUsername("owner")).thenReturn(springUser());
    }

    private ExpenseResponse sampleResponse() {
        return new ExpenseResponse(5L, 10L, "E-1", null, null, 3L, "Supplies",
                "desc", BigDecimal.TEN, LocalDate.of(2026, 1, 15), ExpenseStatus.PAID,
                10L, "owner", null, null);
    }

    @Test
    void createBindsJsonBodyAndReturnsCreated() throws Exception {
        allowJwt();
        when(expenseService.create(any(ExpenseRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerId": 10,
                                  "expenseNo": "E-1",
                                  "categoryId": 3,
                                  "description": "desc",
                                  "amount": 10.00,
                                  "expenseDate": "2026-01-15"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expenseNo").value("E-1"));

        ArgumentCaptor<ExpenseRequest> captor = ArgumentCaptor.forClass(ExpenseRequest.class);
        verify(expenseService).create(captor.capture());
        assertThat(captor.getValue().ownerId()).isEqualTo(10L);
        assertThat(captor.getValue().expenseNo()).isEqualTo("E-1");
        assertThat(captor.getValue().categoryId()).isEqualTo(3L);
        assertThat(captor.getValue().amount()).isEqualByComparingTo("10.00");
    }

    @Test
    void createRejectsInvalidJsonBody() throws Exception {
        allowJwt();

        mockMvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerId": 10}
                                """))
                .andExpect(status().isBadRequest());

        verify(expenseService, never()).create(any(ExpenseRequest.class));
    }

    @Test
    void getByOwnerReturnsForbiddenWhenServiceDeniesAccess() throws Exception {
        allowJwt();
        when(expenseService.getByIdAndOwnerId(eq(5L), eq(99L)))
                .thenThrow(new AccessDeniedException("denied"));

        mockMvc.perform(get("/api/v1/expenses/5")
                        .param("ownerId", "99")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }
}
