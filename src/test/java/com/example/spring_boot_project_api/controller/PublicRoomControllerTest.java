package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.request.PublicRoomQueryRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.PublicOwnerResponse;
import com.example.spring_boot_project_api.dto.response.PublicRoomResponse;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.service.PublicRoomService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(PublicRoomController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class PublicRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicRoomService publicRoomService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private PublicRoomResponse samplePublicRoomResponse(Long id, Long availableCount, BigDecimal fromRent) {
        return new PublicRoomResponse(
                id,
                1L,
                "Deluxe Room",
                "101",
                1,
                2,
                BigDecimal.valueOf(350),
                BigDecimal.valueOf(100),
                RoomStatus.AVAILABLE,
                "Cozy deluxe room",
                7L,
                "Villa Mondul",
                "Street 200",
                "12000",
                "https://image.url/house.jpg",
                3L,
                "Deluxe",
                "https://image.url/roomtype.jpg",
                List.of("https://image.url/roomtype.jpg"),
                List.of("WiFi", "Aircon"),
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                1,
                BigDecimal.valueOf(25),
                null,
                1,
                "Phnom Penh",
                BigDecimal.valueOf(11.5564),
                BigDecimal.valueOf(104.9282),
                "https://maps.google.com",
                List.of(),
                List.of(),
                availableCount,
                fromRent,
                List.of(),
                new PublicOwnerResponse(42L, "Lanh Phalla", "+855 12 345 678",
                        "lanh.phalla@example.com")
        );
    }

    @Test
    void listRoomsShouldReturnGroupedPublicRoomsWithoutAuth() throws Exception {
        PublicRoomResponse groupItem = samplePublicRoomResponse(1L, 3L, BigDecimal.valueOf(300));
        PageResponse<PublicRoomResponse> pageResponse = PageResponse.of(
                List.of(groupItem), 0, 12, 1, 1);

        when(publicRoomService.listRooms(any(PublicRoomQueryRequest.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/public/rooms")
                        .param("search", "Deluxe")
                        .param("cityProvince", "Phnom Penh")
                        .param("propertyType", "ROOM")
                        .param("minRent", "100")
                        .param("maxRent", "500")
                        .param("bedrooms", "1")
                        .param("bathrooms", "1")
                        .param("amenities", "WiFi", "Aircon")
                        .param("page", "0")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].availableRoomCount").value(3))
                .andExpect(jsonPath("$.content[0].fromRent").value(300))
                .andExpect(jsonPath("$.content[0].houseName").value("Villa Mondul"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(publicRoomService).listRooms(any(PublicRoomQueryRequest.class), any(Pageable.class));
    }

    @Test
    void listRoomsShouldReturn400BadRequestForInvalidEnumOrDate() throws Exception {
        mockMvc.perform(get("/api/public/rooms")
                        .param("propertyType", "INVALID_PROPERTY_TYPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRoomShouldReturnPhysicalRoomDetailWithoutAuth() throws Exception {
        PublicRoomResponse detail = samplePublicRoomResponse(1L, null, null);
        when(publicRoomService.getRoom(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/public/rooms/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roomNumber").value("101"))
                .andExpect(jsonPath("$.houseName").value("Villa Mondul"))
                .andExpect(jsonPath("$.availableRooms").isArray())
                .andExpect(jsonPath("$.owner.name").value("Lanh Phalla"))
                .andExpect(jsonPath("$.owner.phone").value("+855 12 345 678"))
                .andExpect(jsonPath("$.owner.email").value("lanh.phalla@example.com"))
                .andExpect(jsonPath("$.availableRoomCount").doesNotExist());

        verify(publicRoomService).getRoom(1L);
    }

    @Test
    void getRoomShouldReturn404WhenNotFoundOrNotPublished() throws Exception {
        when(publicRoomService.getRoom(999L)).thenThrow(RoomNotFoundException.forId(999L));

        mockMvc.perform(get("/api/public/rooms/{id}", 999L))
                .andExpect(status().isNotFound());

        verify(publicRoomService).getRoom(999L);
    }
}
