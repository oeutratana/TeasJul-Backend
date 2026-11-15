package com.example.spring_boot_project_api.repository;

import com.example.spring_boot_project_api.enums.OwnerSystemDefaultStatus;
import com.example.spring_boot_project_api.model.OwnerSystemDefault;
import com.example.spring_boot_project_api.model.SystemDefault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public interface OwnerSystemDefaultRepository extends JpaRepository<OwnerSystemDefault, Long> {
    List<OwnerSystemDefault> findByOwnerId(Long ownerId);

    List<OwnerSystemDefault> findByOwnerIdAndStatus(Long ownerId, OwnerSystemDefaultStatus status);

    Optional<OwnerSystemDefault> findByOwnerIdAndSystemDefault(Long ownerId, SystemDefault systemDefault);
}