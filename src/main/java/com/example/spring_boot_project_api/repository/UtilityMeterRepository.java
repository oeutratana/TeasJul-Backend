package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.UtilityMeter;

@Repository
public interface UtilityMeterRepository
        extends JpaRepository<UtilityMeter, Long>, JpaSpecificationExecutor<UtilityMeter> {

    Optional<UtilityMeter> findByIdAndOwnerId(Long id, Long ownerId);

    List<UtilityMeter> findByHouseId(Long houseId);

    List<UtilityMeter> findByHouseIdIn(Collection<Long> houseIds);

    List<UtilityMeter> findByRoomId(Long roomId);

    boolean existsByOwnerIdAndUtilityTypeIdAndMeterNumber(
            Long ownerId, Long utilityTypeId, String meterNumber);

    boolean existsByOwnerIdAndUtilityTypeIdAndMeterNumberAndIdNot(
            Long ownerId, Long utilityTypeId, String meterNumber, Long id);

    List<UtilityMeter> findByRoomIdAndUtilityTypeId(Long roomId, Long utilityTypeId);
}
