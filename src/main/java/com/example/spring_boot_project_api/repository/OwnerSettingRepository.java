package com.example.spring_boot_project_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.OwnerSetting;

@Repository
public interface OwnerSettingRepository extends JpaRepository<OwnerSetting, Long> {

    Optional<OwnerSetting> findByOwnerId(Long ownerId);

    boolean existsByOwnerId(Long ownerId);
}
