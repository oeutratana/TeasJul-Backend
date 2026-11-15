package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.ManagerMonthlyReportResponse;
import com.example.spring_boot_project_api.service.ManagerReportService;
import com.example.spring_boot_project_api.util.JwtUtil;
import com.example.spring_boot_project_api.util.SecurityUtils;

@WebMvcTest(ManagerReportController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class ManagerReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManagerReportService managerReportService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(true);
    }

    private ManagerMonthlyReportResponse sampleReport() {
        return new ManagerMonthlyReportResponse(
                new ManagerMonthlyReportResponse.ReportMeta(2026, 9,
                        LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0),
                        null, null, LocalDateTime.of(2026, 9, 7, 10, 0)),
                new ManagerMonthlyReportResponse.RoomStats(6, 3, 2, 0, 1, 50.0),
                new ManagerMonthlyReportResponse.FinancialSummary(
                        new BigDecimal("3000.00"), new BigDecimal("1800.00"),
                        new BigDecimal("900.00"), new BigDecimal("500.00"),
                        new BigDecimal("120.00"), new BigDecimal("2180.00")),
                new ManagerMonthlyReportResponse.ContractSummary(4, 25, 2, 1, 3),
                new ManagerMonthlyReportResponse.PaymentSummary(40, 10, 5, new BigDecimal("1800.00")),
                new ManagerMonthlyReportResponse.UtilitySummary(
                        new BigDecimal("150.00"), new BigDecimal("260.00"),
                        new BigDecimal("210.00"), 18, 7),
                new ManagerMonthlyReportResponse.MaintenanceSummary(10, 2, 3, 5, 0, new BigDecimal("120.00")),
                List.of(new ManagerMonthlyReportResponse.MonthlyRevenuePoint("2026-09", new BigDecimal("1800.00"))),
                List.of(new ManagerMonthlyReportResponse.StatusCount("PAID", 30)),
                List.of(new ManagerMonthlyReportResponse.StatusCount("OCCUPIED", 3)),
                List.of(new ManagerMonthlyReportResponse.StatusCount("RESOLVED", 5)));
    }

    @Test
    void monthlyReportRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/manager/reports/monthly").param("year", "2026").param("month", "9"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMonthlyReportReturnsReportForManager() throws Exception {
        when(managerReportService.getMonthlyReport(2026, 9, null)).thenReturn(sampleReport());

        mockMvc.perform(get("/api/manager/reports/monthly")
                        .param("year", "2026")
                        .param("month", "9")
                        .with(user("sok").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.year").value(2026))
                .andExpect(jsonPath("$.meta.month").value(9))
                .andExpect(jsonPath("$.rooms.totalRooms").value(6))
                .andExpect(jsonPath("$.rooms.occupancyRate").value(50.0))
                .andExpect(jsonPath("$.financial.netRevenue").value(2180.00))
                .andExpect(jsonPath("$.contracts.activeContracts").value(25))
                .andExpect(jsonPath("$.payments.paidPayments").value(40))
                .andExpect(jsonPath("$.utilities.totalUtilityCharges").value(210.00))
                .andExpect(jsonPath("$.maintenance.totalRequests").value(10))
                .andExpect(jsonPath("$.revenueTrend[0].month").value("2026-09"))
                .andExpect(jsonPath("$.paymentStatus[0].status").value("PAID"))
                .andExpect(jsonPath("$.roomOccupancy[0].status").value("OCCUPIED"))
                .andExpect(jsonPath("$.maintenanceStatus[0].status").value("RESOLVED"));
    }

    @Test
    void getMonthlyReportForbidsNonManager() throws Exception {
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);

        mockMvc.perform(get("/api/manager/reports/monthly")
                        .param("year", "2026")
                        .param("month", "9")
                        .with(user("sopha").roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMonthlyReportPassesParamsToService() throws Exception {
        when(managerReportService.getMonthlyReport(2026, 9, 8L)).thenReturn(sampleReport());

        mockMvc.perform(get("/api/manager/reports/monthly")
                        .param("year", "2026")
                        .param("month", "9")
                        .param("houseId", "8")
                        .with(user("sok").roles("MANAGER")))
                .andExpect(status().isOk());

        verify(managerReportService).getMonthlyReport(eq(2026), eq(9), eq(8L));
    }
}