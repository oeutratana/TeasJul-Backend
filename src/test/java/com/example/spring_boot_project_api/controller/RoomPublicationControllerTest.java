package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.mapper.RoomMapper;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.service.RoomPublicationService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(RoomPublicationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class RoomPublicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private RoomPublicationService roomPublicationService;

    @MockitoBean
    private RoomMapper roomMapper;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private RoomResponse sampleResponse() {
        RoomResponse response = new RoomResponse();
        response.setId(1L);
        response.setOwnerId(20L);
        response.setHouseId(7L);
        response.setRoomTypeId(3L);
        response.setRoomNumber("101");
        response.setMonthlyRent(java.math.BigDecimal.valueOf(350));
        response.setStatus(RoomStatus.AVAILABLE);
        response.setPublicationStatus(RoomPublicationStatus.PENDING_APPROVAL);
        response.setSubmittedAt(java.time.LocalDateTime.now());
        return response;
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
    void submitShouldReturnPendingApprovalRoom() throws Exception {
        allowJwt();
        when(roomPublicationService.submitForApproval(1L)).thenReturn(new Room());
        when(roomMapper.toDto(any(Room.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/rooms/1/submit")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.roomNumber").value("101"));

        verify(roomPublicationService).submitForApproval(1L);
    }

    @Test
    void approveShouldReturnPublishedRoom() throws Exception {
        allowJwt();
        RoomResponse published = sampleResponse();
        published.setPublicationStatus(RoomPublicationStatus.PUBLISHED);
        when(roomPublicationService.approve(1L)).thenReturn(new Room());
        when(roomMapper.toDto(any(Room.class))).thenReturn(published);

        mockMvc.perform(post("/api/admin/rooms/1/approve")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"));

        verify(roomPublicationService).approve(1L);
    }

    @Test
    void rejectShouldPassReasonToService() throws Exception {
        allowJwt();
        RoomResponse rejected = sampleResponse();
        rejected.setPublicationStatus(RoomPublicationStatus.REJECTED);
        rejected.setRejectionReason("Missing floor plan");
        when(roomPublicationService.reject(eq(1L), eq("Missing floor plan"))).thenReturn(new Room());
        when(roomMapper.toDto(any(Room.class))).thenReturn(rejected);

        mockMvc.perform(post("/api/admin/rooms/1/reject")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Missing floor plan"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Missing floor plan"));

        verify(roomPublicationService).reject(1L, "Missing floor plan");
    }

    @Test
    void rejectShouldRejectBlankReason() throws Exception {
        allowJwt();

        mockMvc.perform(post("/api/admin/rooms/1/reject")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unpublishShouldReturnUnpublishedRoom() throws Exception {
        allowJwt();
        RoomResponse unpublished = sampleResponse();
        unpublished.setPublicationStatus(RoomPublicationStatus.UNPUBLISHED);
        when(roomPublicationService.unpublish(1L)).thenReturn(new Room());
        when(roomMapper.toDto(any(Room.class))).thenReturn(unpublished);

        mockMvc.perform(post("/api/admin/rooms/1/unpublish")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("UNPUBLISHED"));

        verify(roomPublicationService).unpublish(1L);
    }

    @Test
    void listRoomsByPublicationStatusShouldReturnPage() throws Exception {
        allowJwt();
        RoomResponse draft = sampleResponse();
        draft.setPublicationStatus(RoomPublicationStatus.DRAFT);
        PageImpl<Room> page = new PageImpl<>(List.of(new Room()),
                PageRequest.of(0, 10), 1);
        when(roomPublicationService.getRoomsByPublicationStatus(
                eq(RoomPublicationStatus.DRAFT), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(roomMapper.toDto(any(Room.class))).thenReturn(draft);

        mockMvc.perform(get("/api/rooms/publication")
                        .param("status", "DRAFT")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].publicationStatus").value("DRAFT"));
    }

    @Test
    void listRoomsForReviewShouldReturnPage() throws Exception {
        allowJwt();
        RoomResponse pending = sampleResponse();
        pending.setPublicationStatus(RoomPublicationStatus.PENDING_APPROVAL);
        PageImpl<Room> page = new PageImpl<>(List.of(new Room()),
                PageRequest.of(0, 10), 1);
        when(roomPublicationService.getRoomsForReview(
                eq(RoomPublicationStatus.PENDING_APPROVAL), eq(null),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(roomMapper.toDto(any(Room.class))).thenReturn(pending);

        mockMvc.perform(get("/api/admin/rooms/publication")
                        .param("status", "PENDING_APPROVAL")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].roomNumber").value("101"));

        verify(roomPublicationService).getRoomsForReview(
                eq(RoomPublicationStatus.PENDING_APPROVAL), eq(null),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void listRoomsForReviewShouldForwardRoomStatusFilter() throws Exception {
        allowJwt();
        RoomResponse published = sampleResponse();
        published.setPublicationStatus(RoomPublicationStatus.PUBLISHED);
        PageImpl<Room> page = new PageImpl<>(List.of(new Room()),
                PageRequest.of(0, 10), 1);
        when(roomPublicationService.getRoomsForReview(
                eq(RoomPublicationStatus.PUBLISHED), eq(RoomStatus.AVAILABLE),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(roomMapper.toDto(any(Room.class))).thenReturn(published);

        mockMvc.perform(get("/api/admin/rooms/publication")
                        .param("status", "PUBLISHED")
                        .param("roomStatus", "AVAILABLE")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].publicationStatus").value("PUBLISHED"));

        verify(roomPublicationService).getRoomsForReview(
                eq(RoomPublicationStatus.PUBLISHED), eq(RoomStatus.AVAILABLE),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void listRoomsForReviewShouldAcceptOccupiedRoomStatus() throws Exception {
        allowJwt();
        RoomResponse occupied = sampleResponse();
        occupied.setPublicationStatus(RoomPublicationStatus.PUBLISHED);
        occupied.setStatus(RoomStatus.OCCUPIED);
        PageImpl<Room> page = new PageImpl<>(List.of(new Room()),
                PageRequest.of(0, 10), 1);
        when(roomPublicationService.getRoomsForReview(
                eq(RoomPublicationStatus.PUBLISHED), eq(RoomStatus.OCCUPIED),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(roomMapper.toDto(any(Room.class))).thenReturn(occupied);

        mockMvc.perform(get("/api/admin/rooms/publication")
                        .param("status", "PUBLISHED")
                        .param("roomStatus", "OCCUPIED")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("OCCUPIED"));

        verify(roomPublicationService).getRoomsForReview(
                eq(RoomPublicationStatus.PUBLISHED), eq(RoomStatus.OCCUPIED),
                any(org.springframework.data.domain.Pageable.class));
    }
}