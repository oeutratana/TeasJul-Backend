package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.RoomTypeStatus;
import com.example.spring_boot_project_api.model.RoomType;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    List<RoomType> findByOwnerId(Long ownerId);

    List<RoomType> findByOwnerIdIn(Collection<Long> ownerIds);

    Optional<RoomType> findByCode(String code);

    List<RoomType> findByOwnerIdAndStatus(Long ownerId, RoomTypeStatus status);

    boolean existsByCode(String code);
}
