package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.RoomUtility;

@Repository
public interface RoomUtilityRepository
        extends JpaRepository<RoomUtility, Long>, JpaSpecificationExecutor<RoomUtility> {

    List<RoomUtility> findByRoomIdAndStatus(Long roomId, com.example.spring_boot_project_api.enums.UtilityTypeStatus status);

    List<RoomUtility> findByRoomId(Long roomId);

    /** Bulk deletes all utility assignments of a room immediately (used before re-inserting). */
    @Modifying
    @Query("delete from RoomUtility ru where ru.roomId = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);

    List<RoomUtility> findByRoomIdInAndStatus(List<Long> roomIds, com.example.spring_boot_project_api.enums.UtilityTypeStatus status);

    boolean existsByRoomIdAndUtilityTypeIdAndStatus(Long roomId, Long utilityTypeId, com.example.spring_boot_project_api.enums.UtilityTypeStatus status);

    void deleteByRoomIdAndUtilityTypeId(Long roomId, Long utilityTypeId);

    Optional<RoomUtility> findByRoomIdAndUtilityTypeId(Long roomId, Long utilityTypeId);

    List<RoomUtility> findByRoomIdIn(Collection<Long> roomIds);
}
