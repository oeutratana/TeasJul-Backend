package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.FeedbackStatus;

/**
 * Response payload for a single feedback submission about the TeasJul system.
 *
 * <p>{@code ownerName} is the display name of the submitting owner
 * (firstName + lastName joined with a space), resolved by
 * {@link com.example.spring_boot_project_api.mapper.FeedbackMapper}. The
 * value is {@code null} when the owner account has been deleted but the
 * feedback row was retained by the database cascade rule.</p>
 */
public record FeedbackResponse(
        Long id,
        Long ownerId,
        String ownerName,
        Integer rating,
        String comment,
        FeedbackStatus status,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {}