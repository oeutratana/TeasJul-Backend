package com.example.spring_boot_project_api.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.CreateFeedbackRequest;
import com.example.spring_boot_project_api.enums.FeedbackStatus;
import com.example.spring_boot_project_api.model.Feedback;

/**
 * Business operations for owner feedback about the TeasJul system.
 *
 * <p>Submitting a new review is role-gated to authenticated owners through
 * {@code FEEDBACK_CREATE} permission (always granted to OWNER via
 * {@link com.example.spring_boot_project_api.util.SecurityUtils#hasPermission}).
 * Publishing / unpublishing are gated to {@code ROLE_SUPER_ADMIN}, mirroring
 * the room publication approval gate.</p>
 */
public interface FeedbackService {

    /**
     * Submit feedback about the TeasJul system.
     *
     * @param request validated rating + comment
     * @return the persisted feedback in PENDING state
     */
    Feedback submitFeedback(CreateFeedbackRequest request);

    /**
     * Paginated list of the current owner's own submissions.
     *
     * @return ordered by created date descending
     */
    Page<Feedback> getMyFeedback(Pageable pageable);

    /**
     * Paginated list for admin review.
     *
     * @param status  optional status filter; {@code null} returns all
     * @return ordered by created date descending, submitted-at ascending for equal dates
     */
    Page<Feedback> listFeedback(FeedbackStatus status, Pageable pageable);

    /**
     * PENDING/PUBLISHED → PUBLISHED (SUPER_ADMIN only).
     */
    Feedback publishFeedback(Long feedbackId);

    /**
     * PENDING/PUBLISHED → UNPUBLISHED (SUPER_ADMIN only).
     */
    Feedback unpublishFeedback(Long feedbackId);

    /**
     * Fixed-size list of published feedback for the public testimonials display.
     * Maximum of 12 most recent entries, ordered by created date descending.
     */
    List<Feedback> getPublishedFeedback();

    /**
     * Returns a count of feedback submissions in the given status.
     */
    long countByStatus(FeedbackStatus status);
}