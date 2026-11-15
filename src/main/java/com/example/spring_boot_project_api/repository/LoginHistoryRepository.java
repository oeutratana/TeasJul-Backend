package com.example.spring_boot_project_api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_boot_project_api.model.LoginHistory;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    /**
     * Newest-first page of one account's sign-in attempts. Backed by
     * {@code idx_login_history_user_occurred_at}.
     */
    Page<LoginHistory> findByUserIdOrderByOccurredAtDesc(Long userId, Pageable pageable);
}
