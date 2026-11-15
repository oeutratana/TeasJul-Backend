package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.NotificationResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UnreadCountResponse;
import com.example.spring_boot_project_api.enums.NotificationType;
import com.example.spring_boot_project_api.exception.NotificationNotFoundException;
import com.example.spring_boot_project_api.service.NotificationService;
import com.example.spring_boot_project_api.util.JwtUtil;

import org.springframework.security.core.userdetails.User;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private NotificationService notificationService;

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

    private NotificationResponse sampleNotification() {
        return new NotificationResponse(1L, NotificationType.INVOICE_CREATED,
                "New invoice ready", "Invoice INV-2026-0001 (2026-09) has been issued. Total due: $100.00",
                Map.of("entity", "invoice", "id", 5L),
                null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void allNotificationEndpointsRequireAuthentication() throws Exception {
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new UsernameNotFoundException("User not found: ghost"));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/notifications/1/read"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getNotificationsReturnsPagedContentWithValidJwt() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        when(notificationService.getMyNotifications(any(Pageable.class)))
                .thenReturn(PageResponse.of(List.of(sampleNotification()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].type").value("INVOICE_CREATED"))
                .andExpect(jsonPath("$.content[0].data.entity").value("invoice"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getUnreadCountReturnsCount() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        when(notificationService.getUnreadCount()).thenReturn(new UnreadCountResponse(2));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));
    }

    @Test
    void markAsReadReturnsUpdatedNotification() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        when(notificationService.markAsRead(1L))
                .thenReturn(new NotificationResponse(1L, NotificationType.INVOICE_CREATED,
                        "t", "m", Map.of("entity", "invoice", "id", 5L),
                        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(patch("/api/notifications/1/read")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.readAt").isNotEmpty());
    }

    @Test
    void markAsReadReturns404ForNotificationNotOwnedByCurrentUser() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        when(notificationService.markAsRead(99L))
                .thenThrow(NotificationNotFoundException.forId(99L));

        mockMvc.perform(patch("/api/notifications/99/read")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found with id: 99"));
    }

    @Test
    void markAllAsReadReturnsNoContent() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        doNothing().when(notificationService).markAllAsRead();

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNoContent());
    }
}