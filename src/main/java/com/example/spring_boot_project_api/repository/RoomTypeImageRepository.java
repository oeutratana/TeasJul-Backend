package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.RoomTypeImage;

@Repository
public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImage, Long> {

    boolean existsByRoomTypeId(Long roomTypeId);

    List<RoomTypeImage> findByRoomTypeIdOrderBySortOrderAscIdAsc(Long roomTypeId);

    Optional<RoomTypeImage> findByIdAndRoomTypeId(Long id, Long roomTypeId);

    Optional<RoomTypeImage> findFirstByRoomTypeIdOrderBySortOrderAscIdAsc(Long roomTypeId);

    /** Returns the primary image for every room type in the given id set (bulk, to avoid N+1). */
    @Query("SELECT i FROM RoomTypeImage i WHERE i.roomType.id IN :roomTypeIds AND i.isPrimary = true "
            + "ORDER BY i.roomType.id ASC, i.sortOrder ASC, i.id ASC")
    List<RoomTypeImage> findPrimaryByRoomTypeIds(@Param("roomTypeIds") Collection<Long> roomTypeIds);
}