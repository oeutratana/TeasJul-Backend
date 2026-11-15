package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.RoomAmenity;
import com.example.spring_boot_project_api.model.RoomAmenityId;


@Repository
public interface RoomAmenityRepository extends JpaRepository<RoomAmenity, RoomAmenityId> {

    boolean existsByIdRoomIdAndIdAmenityId(Long roomId, Long amenityId);

    @EntityGraph(attributePaths = "amenity")
    List<RoomAmenity> findByIdRoomId(Long roomId);

    Optional<RoomAmenity> findByIdRoomIdAndIdAmenityId(Long roomId, Long amenityId);

    List<RoomAmenity> findByIdRoomIdIn(java.util.Collection<Long> roomIds);

    void deleteByIdRoomIdAndIdAmenityId(Long roomId, Long amenityId);

    /** Bulk deletes every amenity assignment of the given room immediately (used before re-inserting). */
    @Modifying
    @Query("delete from RoomAmenity ra where ra.id.roomId = :roomId")
    void deleteByIdRoomId(@Param("roomId") Long roomId);
}
