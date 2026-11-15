package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.OtpPurpose;
import com.example.spring_boot_project_api.model.OtpVerification;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(
            Long userId, OtpPurpose purpose);

    void deleteByUserIdAndPurpose(Long userId, OtpPurpose purpose);

    long countByUserIdAndPurposeAndCreatedAtAfter(Long userId, OtpPurpose purpose,
            LocalDateTime createdAtAfter);

    List<OtpVerification> findByUserIdAndPurpose(Long userId, OtpPurpose purpose);
}
