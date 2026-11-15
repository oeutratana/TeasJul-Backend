package com.example.spring_boot_project_api.repository;

import com.example.spring_boot_project_api.enums.AmenityStatus;
import com.example.spring_boot_project_api.model.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
    List<Amenity> findByOwnerId(Long ownerId);

    List<Amenity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<Amenity> findByOwnerIdInOrderByCreatedAtDesc(Collection<Long> ownerIds);

    List<Amenity> findByOwnerIdAndStatus(Long ownerId, AmenityStatus status);

    List<Amenity> findByOwnerIdInAndStatus(Collection<Long> ownerIds, AmenityStatus status);

    Optional<Amenity> findByOwnerIdAndCode(Long ownerId, String code);

    Optional<Amenity> findByOwnerIdAndCodeIgnoreCase(Long ownerId, String code);

    Optional<Amenity> findByOwnerIdAndNameIgnoreCase(Long ownerId, String name);

    Optional<Amenity> findByOwnerIdAndName(Long ownerId, String name);

    boolean existsByOwnerIdAndCode(Long ownerId, String code);

    boolean existsByOwnerIdAndName(Long ownerId, String name);

    boolean existsByOwnerIdAndCodeAndIdNot(Long ownerId, String code, Long id);

    boolean existsByOwnerIdAndNameAndIdNot(Long ownerId, String name, Long id);
}
