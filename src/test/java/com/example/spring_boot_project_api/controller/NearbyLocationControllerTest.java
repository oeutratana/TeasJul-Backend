package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.request.CreateNearbyLocationRequest;
import com.example.spring_boot_project_api.dto.request.UpdateNearbyLocationRequest;
import com.example.spring_boot_project_api.dto.response.NearbyLocationResponse;
import com.example.spring_boot_project_api.enums.NearbyLocationCategory;
import com.example.spring_boot_project_api.service.NearbyLocationService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(NearbyLocationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class NearbyLocationControllerTest {

    private static final long HOUSE_ID = 7L;
    private static final long LOCATION_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private NearbyLocationService nearbyLocationService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserDetails springUser() {
        return User.builder()
                .username("boss")
                .password("n/a")
                .roles("OWNER")
                .build();
    }

    private String validToken() throws Exception {
        when(userDetailsService.loadUserByUsername("boss")).thenReturn(springUser());
        return jwtUtil.generateToken("boss");
    }

    private NearbyLocationResponse response() {
        return new NearbyLocationResponse(
                LOCATION_ID, HOUSE_ID, "Royal University",
                NearbyLocationCategory.UNIVERSITY,
                new BigDecimal("1.50"), 12);
    }

    @Test
    void listNearbyLocationsShouldReturnLocations() throws Exception {
        when(nearbyLocationService.getNearbyLocations(HOUSE_ID)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/houses/{houseId}/nearby-locations", HOUSE_ID)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(LOCATION_ID))
                .andExpect(jsonPath("$[0].houseId").value(HOUSE_ID))
                .andExpect(jsonPath("$[0].name").value("Royal University"))
                .andExpect(jsonPath("$[0].category").value("UNIVERSITY"))
                .andExpect(jsonPath("$[0].distanceKm").value(1.50))
                .andExpect(jsonPath("$[0].travelMinutes").value(12));

        verify(nearbyLocationService).getNearbyLocations(HOUSE_ID);
    }

    @Test
    void getNearbyLocationShouldReturnLocation() throws Exception {
        when(nearbyLocationService.getNearbyLocation(HOUSE_ID, LOCATION_ID))
                .thenReturn(response());

        mockMvc.perform(get("/api/houses/{houseId}/nearby-locations/{id}", HOUSE_ID, LOCATION_ID)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Royal University"));

        verify(nearbyLocationService).getNearbyLocation(HOUSE_ID, LOCATION_ID);
    }

    @Test
    void createNearbyLocationShouldReturnCreated() throws Exception {
        when(nearbyLocationService.createNearbyLocation(eq(HOUSE_ID),
                any(CreateNearbyLocationRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/api/houses/{houseId}/nearby-locations", HOUSE_ID)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Royal University",
                                  "category": "UNIVERSITY",
                                  "distanceKm": 1.50,
                                  "travelMinutes": 12
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Royal University"))
                .andExpect(jsonPath("$.category").value("UNIVERSITY"));
    }

    @Test
    void createNearbyLocationShouldRejectInvalidBody() throws Exception {
        mockMvc.perform(post("/api/houses/{houseId}/nearby-locations", HOUSE_ID)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "UNIVERSITY"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateNearbyLocationShouldReturnUpdated() throws Exception {
        NearbyLocationResponse updated = new NearbyLocationResponse(
                LOCATION_ID, HOUSE_ID, "University of Phnom Penh",
                NearbyLocationCategory.UNIVERSITY,
                new BigDecimal("1.80"), 14);
        when(nearbyLocationService.updateNearbyLocation(eq(HOUSE_ID), eq(LOCATION_ID),
                any(UpdateNearbyLocationRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/houses/{houseId}/nearby-locations/{id}", HOUSE_ID, LOCATION_ID)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "University of Phnom Penh",
                                  "distanceKm": 1.80,
                                  "travelMinutes": 14
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("University of Phnom Penh"))
                .andExpect(jsonPath("$.distanceKm").value(1.80));
    }

    @Test
    void deleteNearbyLocationShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/houses/{houseId}/nearby-locations/{id}", HOUSE_ID, LOCATION_ID)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNoContent());

        verify(nearbyLocationService).deleteNearbyLocation(HOUSE_ID, LOCATION_ID);
    }
}