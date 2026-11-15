package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateFeedbackRequest;
import com.example.spring_boot_project_api.enums.FeedbackStatus;
import com.example.spring_boot_project_api.exception.FeedbackNotFoundException;
import com.example.spring_boot_project_api.model.Feedback;
import com.example.spring_boot_project_api.repository.FeedbackRepository;
import com.example.spring_boot_project_api.service.FeedbackService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private static final String ACCESS_DENIED_MSG =
            "You do not have permission to access this resource";

    private final FeedbackRepository feedbackRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public Feedback submitFeedback(CreateFeedbackRequest request) {
        securityUtils.requirePermission(PermissionConstants.FEEDBACK_CREATE);

        Feedback feedback = Feedback.builder()
                .ownerId(securityUtils.getCurrentUserId())
                .rating(request.rating())
                .comment(request.comment().trim())
                .status(FeedbackStatus.PENDING)
                .build();

        return feedbackRepository.save(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Feedback> getMyFeedback(Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.FEEDBACK_VIEW);
        return feedbackRepository.findByOwnerId(
                securityUtils.getCurrentUserId(), sortedPageable(pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Feedback> listFeedback(FeedbackStatus status, Pageable pageable) {
        requireSuperAdmin();
        Pageable sorted = sortedPageable(pageable);
        return status != null
                ? feedbackRepository.findByStatus(status, sorted)
                : feedbackRepository.findAll(sorted);
    }

    @Override
    @Transactional
    public Feedback publishFeedback(Long feedbackId) {
        requireSuperAdmin();
        Feedback feedback = getFeedback(feedbackId);
        if (feedback.getStatus() == FeedbackStatus.PUBLISHED) {
            return feedback;
        }
        feedback.setStatus(FeedbackStatus.PUBLISHED);
        feedback.setReviewedAt(LocalDateTime.now());
        feedback.setReviewedBy(securityUtils.getCurrentUserId());
        return feedbackRepository.save(feedback);
    }

    @Override
    @Transactional
    public Feedback unpublishFeedback(Long feedbackId) {
        requireSuperAdmin();
        Feedback feedback = getFeedback(feedbackId);
        if (feedback.getStatus() == FeedbackStatus.UNPUBLISHED) {
            return feedback;
        }
        feedback.setStatus(FeedbackStatus.UNPUBLISHED);
        feedback.setReviewedAt(LocalDateTime.now());
        feedback.setReviewedBy(securityUtils.getCurrentUserId());
        return feedbackRepository.save(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feedback> getPublishedFeedback() {
        return feedbackRepository.findTop12ByStatusOrderByCreatedAtDesc(FeedbackStatus.PUBLISHED);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(FeedbackStatus status) {
        return feedbackRepository.countByStatus(status);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void requireSuperAdmin() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }
    }

    private Feedback getFeedback(Long feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> FeedbackNotFoundException.forId(feedbackId));
    }

    private Pageable sortedPageable(Pageable pageable) {
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), 100);
        return PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
    }
}