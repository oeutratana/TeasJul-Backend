package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.MaintenanceAssignment;

@Repository
public interface MaintenanceAssignmentRepository extends JpaRepository<MaintenanceAssignment, Long> {

    List<MaintenanceAssignment> findAllByRequestIdOrderByAssignedAtAsc(Long requestId);

    Optional<MaintenanceAssignment> findByIdAndRequestId(Long id, Long requestId);
}
