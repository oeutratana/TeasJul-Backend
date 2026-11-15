package com.example.spring_boot_project_api.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.UtilityRate;
import com.example.spring_boot_project_api.enums.UtilityRateStatus;

@Repository
public interface UtilityRateRepository
        extends JpaRepository<UtilityRate, Long>, JpaSpecificationExecutor<UtilityRate> {

    Optional<UtilityRate> findByIdAndOwnerId(Long id, Long ownerId);

    List<UtilityRate> findByOwnerIdAndHouseIdAndUtilityTypeId(Long ownerId, Long houseId, Long utilityTypeId);

    List<UtilityRate> findByOwnerIdAndHouseIdAndUtilityTypeIdAndIdNot(Long ownerId, Long houseId, Long utilityTypeId, Long id);

    List<UtilityRate> findByHouseIdAndUtilityTypeIdAndStatus(Long houseId, Long utilityTypeId,
            UtilityRateStatus status);

    /** Bulk deletes the current rates for a house/utility immediately (used before re-inserting). */
    @Modifying
    @Query("delete from UtilityRate r where r.houseId = :houseId and r.utilityTypeId = :utilityTypeId")
    void deleteByHouseIdAndUtilityTypeId(@Param("houseId") Long houseId,
            @Param("utilityTypeId") Long utilityTypeId);

    @Query("""
        SELECT r FROM UtilityRate r
        WHERE r.houseId IN :houseIds
          AND r.utilityTypeId IN :utilityTypeIds
          AND r.status = :status
          AND r.effectiveFrom <= :date
          AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date)
        ORDER BY r.houseId ASC, r.utilityTypeId ASC, r.effectiveFrom DESC, r.id DESC
    """)
    List<UtilityRate> findCurrentEffectiveRates(@Param("houseIds") List<Long> houseIds,
            @Param("utilityTypeIds") List<Long> utilityTypeIds,
            @Param("date") LocalDate date,
            @Param("status") UtilityRateStatus status);
}
