package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.House;

@Repository
public interface HouseRepository extends JpaRepository<House, Long> {

    Optional<House> findByIdAndOwnerId(Long id, Long ownerId);

    List<House> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    boolean existsByOwnerRecordId(Long ownerRecordId);

    boolean existsByOwnerIdAndName(Long ownerId, String name);

    boolean existsByOwnerIdAndSlug(Long ownerId, String slug);

    boolean existsByOwnerIdAndCode(Long ownerId, String code);

    boolean existsByOwnerIdAndNameAndIdNot(Long ownerId, String name, Long id);

    boolean existsByOwnerIdAndSlugAndIdNot(Long ownerId, String slug, Long id);

    boolean existsByOwnerIdAndCodeAndIdNot(Long ownerId, String code, Long id);
}
