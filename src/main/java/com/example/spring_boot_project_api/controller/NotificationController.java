package com.example.spring_boot_project_api.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.NotificationResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UnreadCountResponse;
import com.example.spring_boot_project_api.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Notifications are strictly owner-scoped: the current user can only read and
 * update their own notifications. Ownership is always derived from the
 * authenticated principal, never from the request body or path, so user ids in
 * paths/requests are ignored.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications",
        description = "In-app notifications for the current user (owner / manager / tenant)")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "List my notifications",
            description = "Returns the current user's notifications, newest first.")
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getMyNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getMyNotifications(pageable));
    }

    @Operation(summary = "Unread notification count",
            description = "Returns the number of unread notifications for the current user.")
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount());
    }

    @Operation(summary = "Mark a notification as read",
            description = "Marks the notification as read if it belongs to the current user; "
                    + "otherwise returns 404.")
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @Operation(summary = "Mark all notifications as read",
            description = "Marks every unread notification of the current user as read.")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }
}