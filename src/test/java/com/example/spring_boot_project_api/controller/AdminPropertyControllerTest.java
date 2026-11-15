package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import com.example.spring_boot_project_api.dto.response.AdminCatalogResponse;
import com.example.spring_boot_project_api.dto.response.AdminPropertyResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.PropertyOwnerResponse;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.service.AdminPropertyService;
import com.example.spring_boot_project_api.util.JwtUtil;

import org.springframework.data.domain.Pageable;

@WebMvcTest(AdminPropertyController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class AdminPropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private AdminPropertyService adminPropertyService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private AdminPropertyResponse samplePost() {
        AdminPropertyResponse response = new AdminPropertyResponse();
        response.setRoomId(1L);
        response.setHouseId(2L);
        response.setPropertyName("Two-Storey Villa");
        response.setAddress("Street 21, Phnom Penh");
        response.setPropertyType(PropertyType.VILLA);
        response.setListingScope(ListingScope.ENTIRE_PROPERTY);
        response.setPublicationStatus(RoomPublicationStatus.PUBLISHED);
        response.setMonthlyRent(BigDecimal.valueOf(1500));
        response.setOwnerName("Mao Leng");
        response.setAccountOwner(false);
        response.setAmenityIds(List.of());
        response.setAmenityNames(List.of());
        response.setUtilities(List.of());
        response.setNearbyLocations(List.of());
        response.setPhotoUrls(List.of());
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        response.setPublishedAt(LocalDateTime.now());
        return response;
    }

    private PropertyOwnerResponse sampleOwnerRecord() {
        return new PropertyOwnerResponse(9L, "Mao Leng", "owner@gmail.com",
                "+85598765432", "Street 21", null, LocalDateTime.now(), LocalDateTime.now());
    }

    private UserDetails springUser() {
        return User.builder()
                .username("boss")
                .password("n/a")
                .roles("ADMIN")
                .build();
    }

    private String validToken() {
        return jwtUtil.generateToken("boss");
    }

    private void allowJwt() throws Exception {
        when(userDetailsService.loadUserByUsername("boss")).thenReturn(springUser());
    }

    @Test
    void createPropertyShouldReturnCreatedWithValidJwt() throws Exception {
        allowJwt();
        when(adminPropertyService.create(any())).thenReturn(samplePost());

        mockMvc.perform(post("/api/admin/properties")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "propertyType": "VILLA",
                                  "ownerRecord": { "name": "Mao Leng" },
                                  "house": {
                                    "name": "Two-Storey Villa",
                                    "address": "Street 21, Phnom Penh"
                                  },
                                  "monthlyRent": 1500,
                                  "publish": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.ownerName").value("Mao Leng"));
    }

    @Test
    void createPropertyShouldRejectMissingPropertyType() throws Exception {
        allowJwt();

        mockMvc.perform(post("/api/admin/properties")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerRecord": { "name": "Mao Leng" }
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listPropertiesShouldReturnPagedResultWithValidJwt() throws Exception {
        allowJwt();
        when(adminPropertyService.list(eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(PageResponse.of(List.of(samplePost()), 0, 10, 1, 1));

        mockMvc.perform(get("/api/admin/properties")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].roomId").value(1));
    }

    @Test
    void getPropertyShouldReturnPostWithValidJwt() throws Exception {
        allowJwt();
        when(adminPropertyService.get(1L)).thenReturn(samplePost());

        mockMvc.perform(get("/api/admin/properties/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyName").value("Two-Storey Villa"));
    }

    @Test
    void updatePropertyShouldReturnUpdatedPostWithValidJwt() throws Exception {
        allowJwt();
        when(adminPropertyService.update(eq(3L), any())).thenReturn(samplePost());

        mockMvc.perform(put("/api/admin/properties/3")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "propertyType": "VILLA",
                                  "ownerRecord": { "name": "Mao Leng" },
                                  "house": {
                                    "name": "Two-Storey Villa",
                                    "address": "Street 21, Phnom Penh"
                                  },
                                  "monthlyRent": 1600,
                                  "publish": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(1));
    }

    @Test
    void catalogShouldReturnOptionsWithValidJwt() throws Exception {
        allowJwt();
        AdminCatalogResponse catalog = new AdminCatalogResponse(
                List.of(new AdminCatalogResponse.AmenityOption(1L, "Aircon", "AIRCON")),
                List.of(new AdminCatalogResponse.UtilityTypeOption(1L, "Electricity", "kWh")),
                List.of(sampleOwnerRecord()),
                List.of());
        when(adminPropertyService.getCatalog()).thenReturn(catalog);

        mockMvc.perform(get("/api/admin/properties/catalog")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amenities[0].id").value(1))
                .andExpect(jsonPath("$.utilityTypes[0].name").value("Electricity"))
                .andExpect(jsonPath("$.propertyOwners[0].id").value(9));
    }

    @Test
    void listPropertyOwnersShouldReturnPagedRecordsWithValidJwt() throws Exception {
        allowJwt();
        when(adminPropertyService.listPropertyOwners(eq(null), any(Pageable.class)))
                .thenReturn(PageResponse.of(List.of(sampleOwnerRecord()), 0, 10, 1, 1));

        mockMvc.perform(get("/api/admin/properties/owners")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Mao Leng"));
    }

    @Test
    void createPropertyOwnerShouldReturnCreatedWithValidJwt() throws Exception {
        allowJwt();
        when(adminPropertyService.createPropertyOwner(any()))
                .thenReturn(sampleOwnerRecord());

        mockMvc.perform(post("/api/admin/properties/owners")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mao Leng",
                                  "phone": "+85598765432"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void createPropertyOwnerShouldRejectBlankName() throws Exception {
        allowJwt();

        mockMvc.perform(post("/api/admin/properties/owners")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                  {"name": "" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletePropertyOwnerShouldReturnNoContentWithValidJwt() throws Exception {
        allowJwt();
        doNothing().when(adminPropertyService).deletePropertyOwner(5L);

        mockMvc.perform(delete("/api/admin/properties/owners/5")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNoContent());

        verify(adminPropertyService).deletePropertyOwner(5L);
    }

    @Test
    void protectedEndpointShouldReturnUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/admin/properties"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpointShouldRejectInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/admin/properties")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }
}