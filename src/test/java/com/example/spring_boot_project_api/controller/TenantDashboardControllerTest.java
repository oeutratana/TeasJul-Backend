package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

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
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse.TenantDashboardFinancial;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse.TenantDashboardTenant;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse.TenantDashboardUtilities;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceDetailResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceResponse;
import com.example.spring_boot_project_api.dto.response.TenantPaymentHistoryResponse;
import com.example.spring_boot_project_api.dto.response.TenantUtilityTrendResponse;
import com.example.spring_boot_project_api.dto.response.UtilityUsageResponse;
import com.example.spring_boot_project_api.enums.InvoiceItemType;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.exception.InvoiceNotFoundException;
import com.example.spring_boot_project_api.service.TenantDashboardService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(TenantDashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class TenantDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantDashboardService tenantDashboardService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private TenantDashboardResponse sampleDashboard() {
        return new TenantDashboardResponse(
                new TenantDashboardTenant(50L, "Sopha Chan"),
                null,
                null,
                new TenantDashboardFinancial(new BigDecimal("85.00"), new BigDecimal("1200.00"), 1, 0, 3),
                new TenantDashboardUtilities(
                        new UtilityUsageResponse("2026-08", new BigDecimal("15"), "m3"),
                        new UtilityUsageResponse("2026-08", new BigDecimal("75.5"), "kWh")));
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/tenant/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDashboardReturnsTenantDashboard() throws Exception {
        when(tenantDashboardService.getDashboard()).thenReturn(sampleDashboard());

        mockMvc.perform(get("/api/tenant/dashboard").with(user("sopha").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant.name").value("Sopha Chan"))
                .andExpect(jsonPath("$.financial.currentDue").value(85.00))
                .andExpect(jsonPath("$.financial.unpaidInvoices").value(1))
                .andExpect(jsonPath("$.utilities.water.unit").value("m3"))
                .andExpect(jsonPath("$.utilities.electricity.usage").value(75.5));
    }

    @Test
    void getUtilityUsageParsesMonthRange() throws Exception {
        when(tenantDashboardService.getUtilityUsage(any(YearMonth.class), any(YearMonth.class)))
                .thenReturn(new TenantUtilityTrendResponse(
                        List.of(new UtilityUsageResponse("2026-08", new BigDecimal("15"), "m3")),
                        List.of(new UtilityUsageResponse("2026-08", new BigDecimal("75.5"), "kWh"))));

        mockMvc.perform(get("/api/tenant/utilities/usage")
                        .param("from", "2026-03")
                        .param("to", "2026-08")
                        .with(user("sopha").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.water").isArray())
                .andExpect(jsonPath("$.water[0].unit").value("m3"))
                .andExpect(jsonPath("$.electricity[0].usage").value(75.5));
    }

    @Test
    void getUtilityUsageRejectsMalformedMonth() throws Exception {
        mockMvc.perform(get("/api/tenant/utilities/usage")
                        .param("from", "2026-13")
                        .with(user("sopha").roles("USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPaymentHistoryReturnsList() throws Exception {
        when(tenantDashboardService.getPaymentHistory(any(YearMonth.class), any(YearMonth.class)))
                .thenReturn(List.of(
                        new TenantPaymentHistoryResponse("2026-07", new BigDecimal("85.00"),
                                InvoiceStatus.PENDING)));

        mockMvc.perform(get("/api/tenant/payments/history")
                        .param("from", "2026-03")
                        .param("to", "2026-08")
                        .with(user("sopha").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value("2026-07"))
                .andExpect(jsonPath("$[0].amount").value(85.00))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getInvoicesReturnsPagedResult() throws Exception {
        when(tenantDashboardService.getMyInvoices(0, 10))
                .thenReturn(PageResponse.of(
                        List.of(new TenantInvoiceResponse(10L, "INV-2026-0010", "2026-08",
                                new BigDecimal("60"), new BigDecimal("5"), new BigDecimal("20"),
                                new BigDecimal("0"), new BigDecimal("85"), InvoiceStatus.PENDING,
                                LocalDate.of(2026, 8, 5))),
                        0, 10, 1, 1));

        mockMvc.perform(get("/api/tenant/invoices").with(user("sopha").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceNo").value("INV-2026-0010"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getInvoiceDetailReturnsUrlBreakdown() throws Exception {
        when(tenantDashboardService.getMyInvoice(10L))
                .thenReturn(new TenantInvoiceDetailResponse(10L, "INV-2026-0010", "2026-08",
                        "Golden House", "A101",
                        new BigDecimal("60"),
                        List.of(new TenantInvoiceDetailResponse.UtilityDetail(
                                InvoiceItemType.ELECTRICITY,
                                new BigDecimal("20"), "kWh", new BigDecimal("20"))),
                        new BigDecimal("5"), new BigDecimal("90.00"), InvoiceStatus.PENDING,
                        LocalDate.of(2026, 8, 5)));

        mockMvc.perform(get("/api/tenant/invoices/10").with(user("sopha").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomNumber").value("A101"))
                .andExpect(jsonPath("$.utilities[0].type").value("ELECTRICITY"));
    }

    @Test
    void getInvoiceDetailReturns404WhenNotOwned() throws Exception {
        when(tenantDashboardService.getMyInvoice(999L))
                .thenThrow(InvoiceNotFoundException.forId(999L));

        mockMvc.perform(get("/api/tenant/invoices/999").with(user("sopha").roles("USER")))
                .andExpect(status().isNotFound());
    }
}