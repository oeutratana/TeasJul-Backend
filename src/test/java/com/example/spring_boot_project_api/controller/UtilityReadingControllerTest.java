package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
import com.example.spring_boot_project_api.dto.response.MonthlyReadingRowResponse;
import com.example.spring_boot_project_api.enums.ReadingStatusFilter;
import com.example.spring_boot_project_api.service.UtilityReadingService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(UtilityReadingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class UtilityReadingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private UtilityReadingService utilityReadingService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserDetails springUser() {
        return User.builder()
                .username("johndoe")
                .password("n/a")
                .roles("USER")
                .build();
    }

    private String validToken() {
        return jwtUtil.generateToken("johndoe");
    }

    private String authHeader() {
        return "Bearer " + validToken();
    }

    private MonthlyReadingRowResponse sampleRow() {
        return new MonthlyReadingRowResponse(
                101L, "A16", 2, 1L, "House A", "Sok Dara",
                1001L, "16066131", 1L, "WATER", "Water", "m\u00B3",
                999L, new BigDecimal("120"), new BigDecimal("135"), new BigDecimal("15"),
                LocalDate.of(2026, 9, 30));
    }

    private void authenticate() {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
    }

    @Test
    void monthlySheetShouldReturnUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/utility-readings/monthly-sheet")
                        .param("year", "2026")
                        .param("month", "9"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void monthlySheetShouldReturnRowsWithFilters() throws Exception {
        authenticate();
        when(utilityReadingService.getMonthlySheet(1L, 2026, 9, 2, "A16",
                ReadingStatusFilter.NOT_ENTERED)).thenReturn(List.of(sampleRow()));

        mockMvc.perform(get("/api/utility-readings/monthly-sheet")
                        .header("Authorization", authHeader())
                        .param("houseId", "1")
                        .param("year", "2026")
                        .param("month", "9")
                        .param("floor", "2")
                        .param("search", "A16")
                        .param("status", "NOT_ENTERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomNumber").value("A16"))
                .andExpect(jsonPath("$[0].floorNumber").value(2))
                .andExpect(jsonPath("$[0].houseName").value("House A"))
                .andExpect(jsonPath("$[0].tenantName").value("Sok Dara"))
                .andExpect(jsonPath("$[0].utilityTypeCode").value("WATER"))
                .andExpect(jsonPath("$[0].previousReading").value(120))
                .andExpect(jsonPath("$[0].currentReading").value(135))
                .andExpect(jsonPath("$[0].usage").value(15));
    }

    @Test
    void monthlySheetShouldAllowOmittingOptionalFilters() throws Exception {
        authenticate();
        when(utilityReadingService.getMonthlySheet(isNull(), eq(2026), eq(9), isNull(), isNull(),
                isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/utility-readings/monthly-sheet")
                        .header("Authorization", authHeader())
                        .param("year", "2026")
                        .param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(utilityReadingService).getMonthlySheet(
                isNull(), eq(2026), eq(9), isNull(), isNull(), isNull());
    }

    @Test
    void floorsShouldReturnDistinctFloorsForHouse() throws Exception {
        authenticate();
        when(utilityReadingService.getMonthlySheetFloors(1L)).thenReturn(List.of(2, 3, 4));

        mockMvc.perform(get("/api/utility-readings/floors")
                        .header("Authorization", authHeader())
                        .param("houseId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(2))
                .andExpect(jsonPath("$[1]").value(3))
                .andExpect(jsonPath("$[2]").value(4));

        verify(utilityReadingService).getMonthlySheetFloors(1L);
    }

    @Test
    void floorsShouldAllowOmittingHouseId() throws Exception {
        authenticate();
        when(utilityReadingService.getMonthlySheetFloors(isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/utility-readings/floors")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void bulkShouldValidateAndReturnCounts() throws Exception {
        authenticate();
        when(utilityReadingService.createReadingsBulk(any()))
                .thenReturn(new com.example.spring_boot_project_api.dto.response
                        .BulkUtilityReadingResponse(1, 1, 0));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/utility-readings/bulk")
                        .header("Authorization", authHeader())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "houseId": 1,
                                  "billingYear": 2026,
                                  "billingMonth": 9,
                                  "rows": [
                                    {"meterId": 1001, "currentReading": 150},
                                    {"meterId": 1002, "currentReading": 500}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.updatedCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(0));

        verify(utilityReadingService).createReadingsBulk(any());
    }
}