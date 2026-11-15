package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreateFeedbackRequest;
import com.example.spring_boot_project_api.dto.response.FeedbackResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.enums.FeedbackStatus;
import com.example.spring_boot_project_api.mapper.FeedbackMapper;
import com.example.spring_boot_project_api.model.Feedback;
import com.example.spring_boot_project_api.service.FeedbackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Owner submission + SUPER_ADMIN review for system feedback.
 *
 * <p>Owner endpoints ({@code /api/owner/feedback}) are permission-gated
 * server-side via {@link FeedbackService}. Admin endpoints are gated to
 * {@code ROLE_SUPER_ADMIN} only, following the same pattern as the room
 * publication approval workflow.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Feedback",
        description = "Owner feedback about the TeasJul system: submit, list and review.")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final FeedbackMapper feedbackMapper;

    // ---- Owner endpoints ----

    @Operation(summary = "Submit feedback about the TeasJul system (owner only)",
            description = "The owner is resolved from the authenticated JWT. "
                    + "Rating is 1-5, comment up to 1000 characters.")
    @PostMapping("/api/owner/feedback")
    public ResponseEntity<FeedbackResponse> submit(
            @Valid @RequestBody CreateFeedbackRequest request) {
        return ResponseEntity.ok(toResponse(feedbackService.submitFeedback(request)));
    }

    @Operation(summary = "List my feedback submissions (owner only)",
            description = "Paginated list of the current owner's own submissions across all statuses.")
    @GetMapping("/api/owner/feedback")
    public ResponseEntity<PageResponse<FeedbackResponse>> myFeedback(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(toPage(feedbackService.getMyFeedback(pageable)));
    }

    // ---- Admin endpoints ----

    @Operation(summary = "List all feedback for review (SUPER_ADMIN only)",
            description = "Paginated listing of all feedback. Optionally filter by status. "
                    + "Returns owner name, rating, comment, status and created date.")
    @GetMapping("/api/admin/feedback")
    public ResponseEntity<PageResponse<FeedbackResponse>> listFeedback(
            @RequestParam(required = false) FeedbackStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(toPage(feedbackService.listFeedback(status, pageable)));
    }

    @Operation(summary = "Publish feedback (SUPER_ADMIN only)",
            description = "Moves PENDING or UNPUBLISHED feedback to PUBLISHED. "
                    + "Published feedback is displayed on the public Register Business page.")
    @PostMapping("/api/admin/feedback/{id}/publish")
    public ResponseEntity<FeedbackResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(feedbackService.publishFeedback(id)));
    }

    @Operation(summary = "Unpublish feedback (SUPER_ADMIN only)",
            description = "Moves PENDING or PUBLISHED feedback to UNPUBLISHED.")
    @PostMapping("/api/admin/feedback/{id}/unpublish")
    public ResponseEntity<FeedbackResponse> unpublish(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(feedbackService.unpublishFeedback(id)));
    }

    // ---- Helpers ----

    private PageResponse<FeedbackResponse> toPage(Page<Feedback> page) {
        return PageResponse.of(
                page.getContent().stream().map(feedbackMapper::toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return feedbackMapper.toDto(feedback);
    }
}