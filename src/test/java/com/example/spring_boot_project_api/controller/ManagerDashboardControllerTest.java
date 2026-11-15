package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.example.spring_boot_project_api.dto.response.ManagerDashboardResponse;
import com.example.spring_boot_project_api.service.ManagerDashboardService;
import com.example.spring_boot_project_api.util.JwtUtil;
import com.example.spring_boot_project_api.util.SecurityUtils;

@WebMvcTest(ManagerDashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class ManagerDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManagerDashboardService managerDashboardService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
    }

    private ManagerDashboardResponse sampleDashboard() {
        return new ManagerDashboardResponse(
                new ManagerDashboardResponse.Kpis(10, 4, 5, 1,
                        new BigDecimal("1250.00"), 50.0, 3, 2),
                List.of(new ManagerDashboardResponse.RevenuePoint("2026-08", new BigDecimal("1250.00"))),
                List.of(new ManagerDashboardResponse.RoomOccupancyPoint("House #8", 2, 2, 1)),
                List.of(new ManagerDashboardResponse.StatusCount("PAID", 6)),
                List.of(new ManagerDashboardResponse.StatusCount("PENDING", 1)));
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDashboardReturnsScopedKpisForOwner() throws Exception {
        when(managerDashboardService.getDashboard(any(), any(), any(), any(), any()))
                .thenReturn(sampleDashboard());

        mockMvc.perform(get("/api/dashboard").with(user("sok").roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalRooms").value(10))
                .andExpect(jsonPath("$.kpis.occupiedRooms").value(5))
                .andExpect(jsonPath("$.kpis.revenue").value(1250.00))
                .andExpect(jsonPath("$.kpis.occupancyRate").value(50.0))
                .andExpect(jsonPath("$.revenueTrend[0].month").value("2026-08"))
                .andExpect(jsonPath("$.roomOccupancy[0].label").value("House #8"))
                .andExpect(jsonPath("$.paymentStatus[0].status").value("PAID"))
                .andExpect(jsonPath("$.maintenanceStatus[0].status").value("PENDING"));
    }

    @Test
    void getDashboardForbidsNonOwnerOrManager() throws Exception {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        mockMvc.perform(get("/api/dashboard").with(user("sopha").roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDashboardPassesFilterParamsToService() throws Exception {
        when(managerDashboardService.getDashboard(eq(8L), eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 6, 30)), eq(com.example.spring_boot_project_api.enums.RoomStatus.OCCUPIED),
                eq(com.example.spring_boot_project_api.enums.InvoiceStatus.PAID)))
                .thenReturn(sampleDashboard());

        mockMvc.perform(get("/api/dashboard")
                        .param("houseId", "8")
                        .param("fromDate", "2026-04-01")
                        .param("toDate", "2026-06-30")
                        .param("roomStatus", "OCCUPIED")
                        .param("paymentStatus", "PAID")
                        .with(user("sok").roles("OWNER")))
                .andExpect(status().isOk());

        verify(managerDashboardService).getDashboard(8L, LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                com.example.spring_boot_project_api.enums.RoomStatus.OCCUPIED,
                com.example.spring_boot_project_api.enums.InvoiceStatus.PAID);
    }
}