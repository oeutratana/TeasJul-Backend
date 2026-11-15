package com.example.spring_boot_project_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
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
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityMeterResponse;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.service.MeterImportService;
import com.example.spring_boot_project_api.service.UtilityMeterService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(UtilityMeterController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class UtilityMeterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private UtilityMeterService utilityMeterService;

    @MockitoBean
    private MeterImportService meterImportService;

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

    private UtilityMeterResponse sampleMeter() {
        return new UtilityMeterResponse(40L, 15L, 1L, 10L, 2L, "WT-A01", null,
                UtilityMeterStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
    }

    private void stubService(PageResponse<UtilityMeterResponse> response) {
        when(utilityMeterService.getMeters(isNull(), isNull(), isNull(), isNull(), isNull(),
                any(Pageable.class)))
                .thenReturn(response);
    }

    @Test
    void listMetersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/utility-meters"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void defaultPageableUsesDeterministicSort() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        stubService(PageResponse.of(List.of(), 0, 10, 0, 0));

        mockMvc.perform(get("/api/utility-meters")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(utilityMeterService).getMeters(isNull(), isNull(), isNull(), isNull(), isNull(),
                captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    @Test
    void pageTwoForwardsDifferentOffset() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        stubService(PageResponse.of(List.of(sampleMeter()), 1, 10, 20, 2));

        mockMvc.perform(get("/api/utility-meters")
                        .param("page", "1")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(utilityMeterService).getMeters(isNull(), isNull(), isNull(), isNull(), isNull(),
                captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getOffset()).isEqualTo(10);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    void listMetersReturnsPagedResponseFields() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        stubService(PageResponse.of(List.of(sampleMeter()), 0, 10, 20, 2));

        mockMvc.perform(get("/api/utility-meters")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(40))
                .andExpect(jsonPath("$.content[0].meterNumber").value("WT-A01"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(20))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
}