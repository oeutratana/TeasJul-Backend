package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.NearbyLocation;

@Repository
public interface NearbyLocationRepository extends JpaRepository<NearbyLocation, Long> {

    List<NearbyLocation> findByHouseIdOrderByDistanceKmAscIdAsc(Long houseId);

    Optional<NearbyLocation> findByIdAndHouseId(Long id, Long houseId);

    List<NearbyLocation> findByHouseIdIn(Collection<Long> houseIds);

    /** Bulk deletes every nearby location of the given house immediately (used before re-inserting). */
    @Modifying
    @Query("delete from NearbyLocation n where n.house.id = :houseId")
    void deleteByHouseId(@Param("houseId") Long houseId);
}