package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.FeedbackResponse;
import com.example.spring_boot_project_api.mapper.FeedbackMapper;
import com.example.spring_boot_project_api.service.FeedbackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Public read-only endpoint for published owner testimonials.
 *
 * <p>No authentication is required. Only {@code PUBLISHED} feedback is
 * exposed -- pending and unpublished rows are never returned by this
 * endpoint.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Public Feedback",
        description = "Read-only published owner testimonials displayed on the Register Business page.")
public class PublicFeedbackController {

    private final FeedbackService feedbackService;
    private final FeedbackMapper feedbackMapper;

    @Operation(summary = "List published owner testimonials",
            description = "Returns only PUBLISHED feedback submitted by owners about the TeasJul system. "
                    + "A maximum of 12 entries is returned.")
    @GetMapping("/api/public/feedback")
    public ResponseEntity<List<FeedbackResponse>> publishedFeedback() {
        return ResponseEntity.ok(
                feedbackService.getPublishedFeedback()
                        .stream()
                        .map(feedbackMapper::toDto)
                        .toList());
    }
}