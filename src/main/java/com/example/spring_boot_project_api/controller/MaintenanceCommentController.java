package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceCommentRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceCommentResponse;
import com.example.spring_boot_project_api.service.MaintenanceCommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/maintenance-requests/{requestId}/comments")
@RequiredArgsConstructor
@Tag(name = "Maintenance Comments",
        description = "Maintenance comment endpoints (request ownership is verified against the authenticated user)")
public class MaintenanceCommentController {

    private final MaintenanceCommentService commentService;

    @Operation(summary = "Add a comment to a maintenance request",
            description = "Adds a comment to the given maintenance request if it belongs to the currently "
                    + "authenticated owner. The authenticated user is recorded as the comment author.")
    @PostMapping
    public ResponseEntity<MaintenanceCommentResponse> createComment(@PathVariable Long requestId,
            @Valid @RequestBody CreateMaintenanceCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(requestId, request));
    }

    @Operation(summary = "List comments of a maintenance request",
            description = "Returns all comments of the maintenance request in chronological order "
                    + "if it belongs to the currently authenticated owner.")
    @GetMapping
    public ResponseEntity<List<MaintenanceCommentResponse>> getComments(@PathVariable Long requestId) {
        return ResponseEntity.ok(commentService.getComments(requestId));
    }

    @Operation(summary = "Delete a comment",
            description = "Deletes a comment if it belongs to the maintenance request "
                    + "of the currently authenticated owner.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long requestId,
            @PathVariable Long commentId) {
        commentService.deleteComment(requestId, commentId);
        return ResponseEntity.noContent().build();
    }
}
