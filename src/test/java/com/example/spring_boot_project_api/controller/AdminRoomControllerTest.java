package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.example.spring_boot_project_api.dto.request.AdminBulkCreateRoomRequest;
import com.example.spring_boot_project_api.dto.request.AdminCreateRoomRequest;
import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.RoomTypeRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.dto.response.RoomTypeResponse;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.model.HouseStatus;
import com.example.spring_boot_project_api.service.AdminRoomService;
import com.example.spring_boot_project_api.util.HouseResponseTestBuilder;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(AdminRoomController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class AdminRoomControllerTest {

    private static final long OWNER_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private AdminRoomService adminRoomService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserDetails springUser() {
        return User.builder()
                .username("boss")
                .password("n/a")
                .roles("ADMIN")
                .build();
    }

    private String validToken() throws Exception {
        when(userDetailsService.loadUserByUsername("boss")).thenReturn(springUser());
        return jwtUtil.generateToken("boss");
    }

    private HouseResponse houseResponse() {
        return HouseResponseTestBuilder.houseResponse()
                .id(7L)
                .ownerId(OWNER_ID)
                .name("Villa Mondul")
                .slug("villa-mondul")
                .code("H001")
                .address("Phnom Penh")
                .totalRooms(5)
                .availableRooms(3)
                .description("Nice villa")
                .status(HouseStatus.ACTIVE)
                .build();
    }

    private RoomTypeResponse roomTypeResponse() {
        RoomTypeResponse response = new RoomTypeResponse();
        response.setId(3L);
        response.setOwnerId(OWNER_ID);
        response.setName("Deluxe");
        response.setCode("DELUXE");
        return response;
    }

    private RoomResponse roomResponse(RoomPublicationStatus publicationStatus) {
        RoomResponse response = new RoomResponse();
        response.setId(1L);
        response.setOwnerId(OWNER_ID);
        response.setHouseId(7L);
        response.setRoomTypeId(3L);
        response.setRoomNumber("101");
        response.setName("Sunset Room");
        response.setMonthlyRent(BigDecimal.valueOf(350));
        response.setStatus(RoomStatus.AVAILABLE);
        response.setPublicationStatus(publicationStatus);
        return response;
    }

    @Test
    void ownerHousesShouldReturnHouses() throws Exception {
        when(adminRoomService.getOwnerHouses(OWNER_ID)).thenReturn(List.of(houseResponse()));

        mockMvc.perform(get("/api/admin/owners/{ownerId}/houses", OWNER_ID)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].ownerId").value(OWNER_ID))
                .andExpect(jsonPath("$[0].name").value("Villa Mondul"));

        verify(adminRoomService).getOwnerHouses(OWNER_ID);
    }

    @Test
    void ownerRoomTypesShouldReturnRoomTypes() throws Exception {
        when(adminRoomService.getOwnerRoomTypes(OWNER_ID)).thenReturn(List.of(roomTypeResponse()));

        mockMvc.perform(get("/api/admin/owners/{ownerId}/room-types", OWNER_ID)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].ownerId").value(OWNER_ID))
                .andExpect(jsonPath("$[0].name").value("Deluxe"));

        verify(adminRoomService).getOwnerRoomTypes(OWNER_ID);
    }

    @Test
    void createRoomShouldReturnCreatedRoom() throws Exception {
        RoomResponse draft = roomResponse(RoomPublicationStatus.DRAFT);
        when(adminRoomService.createRoomForOwner(eq(OWNER_ID), any(AdminCreateRoomRequest.class)))
                .thenReturn(draft);

        mockMvc.perform(post("/api/admin/owners/{ownerId}/rooms", OWNER_ID)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "houseId": 7,
                                  "roomTypeId": 3,
                                  "roomNumber": "101",
                                  "name": "Sunset Room",
                                  "monthlyRent": 350.00,
                                  "publishImmediately": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomNumber").value("101"))
                .andExpect(jsonPath("$.publicationStatus").value("DRAFT"));

        verify(adminRoomService).createRoomForOwner(eq(OWNER_ID), any(AdminCreateRoomRequest.class));
    }

    @Test
    void createRoomWithPublishImmediatelyShouldReturnPublishedRoom() throws Exception {
        RoomResponse published = roomResponse(RoomPublicationStatus.PUBLISHED);
        when(adminRoomService.createRoomForOwner(eq(OWNER_ID), any(AdminCreateRoomRequest.class)))
                .thenReturn(published);

        mockMvc.perform(post("/api/admin/owners/{ownerId}/rooms", OWNER_ID)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "houseId": 7,
                                  "roomTypeId": 3,
                                  "roomNumber": "201",
                                  "monthlyRent": 400.00,
                                  "publishImmediately": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"));
    }

    @Test
    void createRoomShouldRejectInvalidBody() throws Exception {
        mockMvc.perform(post("/api/admin/owners/{ownerId}/rooms", OWNER_ID)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomNumber": "101"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRoomsShouldReturnCreatedRooms() throws Exception {
        when(adminRoomService.createRoomsForOwner(eq(OWNER_ID), any(AdminBulkCreateRoomRequest.class)))
                .thenReturn(List.of(roomResponse(RoomPublicationStatus.DRAFT)));

        mockMvc.perform(post("/api/admin/owners/{ownerId}/rooms/bulk", OWNER_ID)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "publishImmediately": false,
                                  "rooms": [
                                    {
                                      "houseId": 7,
                                      "roomTypeId": 3,
                                      "roomNumber": "101",
                                      "monthlyRent": 350.00
                                    },
                                    {
                                      "houseId": 7,
                                      "roomTypeId": 3,
                                      "roomNumber": "102",
                                      "monthlyRent": 350.00
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].roomNumber").value("101"))
                .andExpect(jsonPath("$[0].publicationStatus").value("DRAFT"));

        verify(adminRoomService).createRoomsForOwner(eq(OWNER_ID), any(AdminBulkCreateRoomRequest.class));
    }

    @Test
    void createRoomsShouldRejectInvalidBody() throws Exception {
        mockMvc.perform(post("/api/admin/owners/{ownerId}/rooms/bulk", OWNER_ID)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rooms": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createHouseForOwnerShouldReturnCreatedHouse() throws Exception {
        when(adminRoomService.createHouseForOwner(eq(OWNER_ID), any(CreateHouseRequest.class)))
                .thenReturn(houseResponse());

        mockMvc.perform(post("/api/admin/owners/{ownerId}/houses", OWNER_ID)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Villa Mondul",
                                  "slug": "villa-mondul",
                                  "code": "H001",
                                  "address": "Phnom Penh"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Villa Mondul"))
                .andExpect(jsonPath("$.ownerId").value(OWNER_ID));

        verify(adminRoomService).createHouseForOwner(eq(OWNER_ID), any(CreateHouseRequest.class));
    }

    @Test
    void createHouseForOwnerShouldRejectInvalidBody() throws Exception {
        mockMvc.perform(post("/api/admin/owners/{ownerId}/houses", OWNER_ID)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Villa Mondul"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRoomTypeForOwnerShouldReturnCreatedRoomType() throws Exception {
        when(adminRoomService.createRoomTypeForOwner(eq(OWNER_ID), any(RoomTypeRequest.class)))
                .thenReturn(roomTypeResponse());

        mockMvc.perform(post("/api/admin/owners/{ownerId}/room-types", OWNER_ID)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Deluxe",
                                  "code": "DELUXE",
                                  "capacity": 2,
                                  "defaultRent": 350.00,
                                  "defaultDeposit": 500.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Deluxe"))
                .andExpect(jsonPath("$.ownerId").value(OWNER_ID));

        verify(adminRoomService).createRoomTypeForOwner(eq(OWNER_ID), any(RoomTypeRequest.class));
    }

    @Test
    void createRoomTypeForOwnerShouldRejectInvalidBody() throws Exception {
        mockMvc.perform(post("/api/admin/owners/{ownerId}/room-types", OWNER_ID)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Deluxe"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}