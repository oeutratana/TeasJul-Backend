package com.example.spring_boot_project_api.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.ContractTenant;

@Repository
public interface ContractTenantRepository extends JpaRepository<ContractTenant, Long> {
    Optional<ContractTenant> findByContractIdAndTenantId(Long contractId, Long tenantId);

    List<ContractTenant> findByContractId(Long contractId);

    List<ContractTenant> findByContractIdIn(Collection<Long> contractIds);

    List<ContractTenant> findByTenantId(Long tenantId);

    boolean existsByContractIdAndTenantId(Long contractId, Long tenantId);

    @Query("""
        SELECT COUNT(ct) > 0
        FROM ContractTenant ct, RentalContract rc
        WHERE ct.contractId = rc.id
          AND ct.tenantId = :tenantId
          AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.ACTIVE
          AND (ct.leftAt IS NULL OR ct.leftAt >= :currentDate)
    """)
    boolean isTenantActivelyRenting(
            @Param("tenantId") Long tenantId,
            @Param("currentDate") LocalDate currentDate
    );

    @Query("SELECT COUNT(DISTINCT ct.tenantId) FROM ContractTenant ct, RentalContract rc, Room r " +
            "WHERE ct.contractId = rc.id AND rc.roomId = r.id " +
            "AND r.houseId = :houseId AND rc.ownerId = :ownerId " +
            "AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.ACTIVE " +
            "AND ct.joinedAt <= :currentDate " +
            "AND (ct.leftAt IS NULL OR ct.leftAt >= :currentDate)")
    long countActiveTenantsByHouseIdAndOwnerId(
            @Param("houseId") Long houseId,
            @Param("ownerId") Long ownerId,
            @Param("currentDate") LocalDate currentDate
    );

    @Query("SELECT COUNT(DISTINCT ct.tenantId) FROM ContractTenant ct, RentalContract rc, Room r " +
            "WHERE ct.contractId = rc.id AND rc.roomId = r.id " +
            "AND r.houseId = :houseId " +
            "AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.ACTIVE " +
            "AND ct.joinedAt <= :currentDate " +
            "AND (ct.leftAt IS NULL OR ct.leftAt >= :currentDate)")
    long countActiveTenantsByHouseId(
            @Param("houseId") Long houseId,
            @Param("currentDate") LocalDate currentDate
    );
}
