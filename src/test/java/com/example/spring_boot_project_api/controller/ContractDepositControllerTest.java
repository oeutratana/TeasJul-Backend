package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.request.CreateDepositRequest;
import com.example.spring_boot_project_api.dto.response.ContractDepositResponse;
import com.example.spring_boot_project_api.enums.ContractDepositStatus;
import com.example.spring_boot_project_api.service.ContractDepositService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(ContractDepositController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class ContractDepositControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private ContractDepositService depositService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void authenticate() {
        UserDetails user = User.builder()
                .username("johndoe")
                .password("n/a")
                .roles("USER")
                .build();
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(user);
    }

    private String authHeader() {
        return "Bearer " + jwtUtil.generateToken("johndoe");
    }

    @Test
    void createShouldReturnUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/v1/contract_deposit")
                        .contentType("application/json")
                        .content("{\"contractId\":1,\"amount\":150}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithAlreadyPaidBindsStatusAndPaidDateAndReturnsPaid() throws Exception {
        when(depositService.createDeposit(any(CreateDepositRequest.class)))
                .thenReturn(paidResponse());

        mockMvc.perform(post("/api/v1/contract_deposit")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content("""
                                {
                                  "contractId": 1,
                                  "amount": 150,
                                  "status": "PAID",
                                  "paidAt": "2026-09-19"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(150))
                .andExpect(jsonPath("$.paidAmount").value(150))
                .andExpect(jsonPath("$.status").value("PAID"));

        var captor = org.mockito.ArgumentCaptor.forClass(CreateDepositRequest.class);
        verify(depositService).createDeposit(captor.capture());
        CreateDepositRequest request = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(request.status()).isEqualTo(ContractDepositStatus.PAID);
        org.assertj.core.api.Assertions.assertThat(request.paidAt()).isEqualTo(LocalDate.of(2026, 9, 19));
    }

    @Test
    void createWithoutOptionalFieldsDefaultsStatusToNullAndBinds() throws Exception {
        when(depositService.createDeposit(any(CreateDepositRequest.class)))
                .thenReturn(pendingResponse());

        mockMvc.perform(post("/api/v1/contract_deposit")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content("{\"contractId\":1,\"amount\":150}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        var captor = org.mockito.ArgumentCaptor.forClass(CreateDepositRequest.class);
        verify(depositService).createDeposit(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().status()).isNull();
        org.assertj.core.api.Assertions.assertThat(captor.getValue().paidAt()).isNull();
    }

    @Test
    void getDepositsByContractReturnsPaidAndPendingRows() throws Exception {
        when(depositService.getDepositsByContract(1L))
                .thenReturn(List.of(paidResponse(), pendingResponse()));

        mockMvc.perform(get("/api/v1/contract_deposit/contract/1")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paidAmount").value(150))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[1].paidAmount").value(0))
                .andExpect(jsonPath("$[1].status").value("PENDING"));
    }

    private ContractDepositResponse paidResponse() {
        return new ContractDepositResponse(
                1L, 1L, new BigDecimal("150.00"), new BigDecimal("150.00"),
                ContractDepositStatus.PAID,
                LocalDateTime.of(2026, 9, 19, 0, 0), null, null,
                LocalDateTime.of(2026, 9, 19, 0, 0), LocalDateTime.of(2026, 9, 19, 0, 0));
    }

    private ContractDepositResponse pendingResponse() {
        return new ContractDepositResponse(
                2L, 1L, new BigDecimal("150.00"), BigDecimal.ZERO,
                ContractDepositStatus.PENDING,
                null, null, null,
                LocalDateTime.of(2026, 9, 19, 0, 0), LocalDateTime.of(2026, 9, 19, 0, 0));
    }
}