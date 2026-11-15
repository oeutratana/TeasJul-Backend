package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.MaintenanceCategory;

@Repository
public interface MaintenanceCategoryRepository extends JpaRepository<MaintenanceCategory, Long> {

    Optional<MaintenanceCategory> findByIdAndOwnerId(Long id, Long ownerId);

    List<MaintenanceCategory> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<MaintenanceCategory> findAllByOwnerIdInOrderByCreatedAtDesc(Collection<Long> ownerIds);

    boolean existsByOwnerIdAndName(Long ownerId, String name);

    boolean existsByOwnerIdAndCode(Long ownerId, String code);

    boolean existsByOwnerIdAndNameAndIdNot(Long ownerId, String name, Long id);

    boolean existsByOwnerIdAndCodeAndIdNot(Long ownerId, String code, Long id);
}
