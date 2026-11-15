package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for an owner submitting feedback about the TeasJul system.
 *
 * <p>{@code rating} is a 1-5 integer (star rating). {@code comment} is free
 * text up to 1000 characters. The owner is resolved from the authenticated
 * JWT -- no owner id is accepted from the client.</p>
 */
public record CreateFeedbackRequest(
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        Integer rating,

        @NotBlank(message = "Comment is required")
        @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
        String comment
) {}