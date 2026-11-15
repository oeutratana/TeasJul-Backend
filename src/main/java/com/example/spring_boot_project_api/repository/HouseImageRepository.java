package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.HouseImage;

@Repository
public interface HouseImageRepository extends JpaRepository<HouseImage, Long> {

    List<HouseImage> findByHouseIdOrderBySortOrderAscIdAsc(Long houseId);

    Optional<HouseImage> findByIdAndHouseId(Long id, Long houseId);

    Optional<HouseImage> findFirstByHouseIdOrderBySortOrderAscIdAsc(Long houseId);

    Optional<HouseImage> findFirstByHouseIdAndIsPrimaryTrue(Long houseId);

    /** Returns the primary image url for every house in the given id set (bulk, to avoid N+1). */
    @Query("SELECT i FROM HouseImage i WHERE i.house.id IN :houseIds AND i.isPrimary = true "
            + "ORDER BY i.house.id ASC, i.sortOrder ASC, i.id ASC")
    List<HouseImage> findPrimaryByHouseIds(@Param("houseIds") Collection<Long> houseIds);
}