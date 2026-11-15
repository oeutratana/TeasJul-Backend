package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.FeedbackStatus;
import com.example.spring_boot_project_api.model.Feedback;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Feedback> findByStatus(FeedbackStatus status, Pageable pageable);

    List<Feedback> findTop12ByStatusOrderByCreatedAtDesc(FeedbackStatus status);

    long countByStatus(FeedbackStatus status);
}