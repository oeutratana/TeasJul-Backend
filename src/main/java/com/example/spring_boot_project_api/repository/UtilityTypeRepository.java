package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.UtilityType;

@Repository
public interface UtilityTypeRepository
        extends JpaRepository<UtilityType, Long>, JpaSpecificationExecutor<UtilityType> {

    Optional<UtilityType> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<UtilityType> findByOwnerIdAndCode(Long ownerId, String code);

    Optional<UtilityType> findByOwnerIdAndCodeIgnoreCase(Long ownerId, String code);

    Optional<UtilityType> findByOwnerIdAndNameIgnoreCase(Long ownerId, String name);

    List<UtilityType> findByOwnerIdIn(Collection<Long> ownerIds);

    List<UtilityType> findByIdInAndStatus(Collection<Long> ids,
            com.example.spring_boot_project_api.enums.UtilityTypeStatus status);

    boolean existsByOwnerIdAndName(Long ownerId, String name);

    boolean existsByOwnerIdAndCode(Long ownerId, String code);

    boolean existsByOwnerIdAndNameAndIdNot(Long ownerId, String name, Long id);

    boolean existsByOwnerIdAndCodeAndIdNot(Long ownerId, String code, Long id);
}
