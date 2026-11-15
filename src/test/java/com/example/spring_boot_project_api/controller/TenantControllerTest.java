package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.example.spring_boot_project_api.dto.response.TenantMeContract;
import com.example.spring_boot_project_api.dto.response.TenantMeHouse;
import com.example.spring_boot_project_api.dto.response.TenantMeResponse;
import com.example.spring_boot_project_api.dto.response.TenantMeRoom;
import com.example.spring_boot_project_api.dto.response.TenantUtilityResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.service.TenantPortalService;
import com.example.spring_boot_project_api.service.TenantService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(TenantController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;
    @MockitoBean
    private TenantPortalService tenantPortalService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private TenantMeResponse sampleProfile() {
        TenantMeRoom room = new TenantMeRoom(12L, "A101", "Room 101", 1,
                new BigDecimal("150000"), new BigDecimal("300000"));
        TenantMeHouse house = new TenantMeHouse(8L, "Golden House", "GH-01", "Phnom Penh");
        TenantMeContract contract = new TenantMeContract(900L, "C-001",
                LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(10),
                new BigDecimal("150000"), new BigDecimal("300000"), 5,
                new BigDecimal("300000"), true, RentalContractStatus.ACTIVE);
        return new TenantMeResponse(50L, 10L, 1L, "Owner A", "099888777", "owner@example.com",
                "TN-A01", "Sopha", "Chan", null, null, "012000111", "sopha@example.com",
                null, null, null, null, null, null, TenantStatus.RENTING,
                house, room, contract, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMeReturnsPortalProfile() throws Exception {
        when(tenantPortalService.getMyTenantProfile()).thenReturn(sampleProfile());

        mockMvc.perform(get("/api/v1/tenants/me").with(user("sopha").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("RENTING"))
                .andExpect(jsonPath("$.ownerName").value("Owner A"))
                .andExpect(jsonPath("$.house.name").value("Golden House"))
                .andExpect(jsonPath("$.room.roomNumber").value("A101"))
                .andExpect(jsonPath("$.contract.contractNo").value("C-001"))
                .andExpect(jsonPath("$.contract.depositFullyPaid").value(true));
    }

    @Test
    void putMeUpdatesPortalProfile() throws Exception {
        when(tenantPortalService.updateMyTenantProfile(any())).thenReturn(sampleProfile());

        mockMvc.perform(put("/api/v1/tenants/me")
                        .with(user("sopha").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Sopha",
                                  "lastName": "Chan",
                                  "phone": "012111222"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.contract.contractNo").value("C-001"));
    }

    @Test
    void getMyUtilitiesReturnsUtilityList() throws Exception {
        when(tenantPortalService.getMyUtilities()).thenReturn(List.of(
                new TenantUtilityResponse(7L, "M-100", "Electricity", "kWh",
                        new BigDecimal("1250.5"), new BigDecimal("75.5"), LocalDate.now().minusDays(2))));

        mockMvc.perform(get("/api/v1/tenants/me/utilities").with(user("sopha").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].meterNumber").value("M-100"))
                .andExpect(jsonPath("$[0].utilityTypeName").value("Electricity"));
    }

    @Test
    void createTenantRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/tenants").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantCode":"TN-001","firstName":"A","lastName":"B","phone":"012"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}