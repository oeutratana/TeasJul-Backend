package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.MaintenanceComment;

@Repository
public interface MaintenanceCommentRepository extends JpaRepository<MaintenanceComment, Long> {

    List<MaintenanceComment> findAllByRequestIdOrderByCreatedAtAsc(Long requestId);

    Optional<MaintenanceComment> findByIdAndRequestId(Long id, Long requestId);
}
