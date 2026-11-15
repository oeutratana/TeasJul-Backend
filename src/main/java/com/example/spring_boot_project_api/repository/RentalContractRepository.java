package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.dto.response.ActiveRoomTenantDto;
import com.example.spring_boot_project_api.dto.response.ExpiringContractSummary;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.model.RentalContract;

@Repository
public interface RentalContractRepository extends JpaRepository<RentalContract, Long> {

    Optional<RentalContract> findByOwnerIdAndContractNo(Long ownerId, String contractNo);

    List<RentalContract> findByOwnerId(Long ownerId);

    List<RentalContract> findByOwnerIdAndRoomId(Long ownerId, Long roomId);

    List<RentalContract> findByOwnerIdAndStatus(Long ownerId, RentalContractStatus status);

    List<RentalContract> findByOwnerIdInAndStatus(Collection<Long> ownerIds, RentalContractStatus status);

    List<RentalContract> findByRoomId(Long roomId);

    List<RentalContract> findByStatus(RentalContractStatus status);

    boolean existsByRoomIdAndStatus(Long roomId, RentalContractStatus status);

    boolean existsByBookingId(Long bookingId);

    boolean existsByContractNo(String contractNo);

    // ---- Contract expiration ----

    // Active contracts whose end date has already passed must be recognized as
    // EXPIRED (end date is inclusive: a contract ending today is still active).
    @Query("""
        SELECT DISTINCT rc.roomId
        FROM RentalContract rc
        WHERE rc.status = :active
          AND rc.endDate IS NOT NULL
          AND rc.endDate < :today
    """)
    List<Long> findRoomIdsOfActiveExpiredContracts(
            @Param("active") RentalContractStatus active,
            @Param("today") LocalDate today);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE RentalContract rc
        SET rc.status = :expired,
            rc.updatedAt = CURRENT_TIMESTAMP
        WHERE rc.status = :active
          AND rc.endDate IS NOT NULL
          AND rc.endDate < :today
    """)
    int expireActiveContractsEndedBefore(
            @Param("active") RentalContractStatus active,
            @Param("expired") RentalContractStatus expired,
            @Param("today") LocalDate today);

    @Query("SELECT DISTINCT rc FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId IN :houseIds " +
            "ORDER BY rc.updatedAt DESC")
    List<RentalContract> findByRoomHouseIdIn(@Param("houseIds") Collection<Long> houseIds);

    @Query("SELECT DISTINCT rc FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId IN :houseIds AND rc.status = :status " +
            "ORDER BY rc.updatedAt DESC")
    List<RentalContract> findByRoomHouseIdInAndStatus(
            @Param("houseIds") Collection<Long> houseIds,
            @Param("status") RentalContractStatus status);

    @Query("""
        SELECT new com.example.spring_boot_project_api.dto.response.ActiveRoomTenantDto(
            rc.roomId,
            t.id,
            t.firstName,
            t.lastName,
            ct.isPrimary
        )
        FROM RentalContract rc, ContractTenant ct, Tenant t
        WHERE rc.id = ct.contractId
          AND ct.tenantId = t.id
          AND rc.roomId IN :roomIds
          AND rc.ownerId IN :ownerIds
          AND t.owner.id IN :ownerIds
          AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.ACTIVE
          AND ct.joinedAt <= :currentDate
          AND (ct.leftAt IS NULL OR ct.leftAt >= :currentDate)
        ORDER BY ct.isPrimary DESC, ct.joinedAt ASC, ct.id ASC
    """)
    List<ActiveRoomTenantDto> findActiveTenantsByRoomIds(
            @Param("roomIds") Collection<Long> roomIds,
            @Param("ownerIds") Collection<Long> ownerIds,
            @Param("currentDate") LocalDate currentDate
    );

    @Query("SELECT COUNT(rc) FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId = :houseId AND rc.ownerId = :ownerId " +
            "AND rc.status = :status")
    long countByHouseIdAndOwnerIdAndStatus(
            @Param("houseId") Long houseId,
            @Param("ownerId") Long ownerId,
            @Param("status") RentalContractStatus status);

    @Query("SELECT rc FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId = :houseId AND rc.ownerId = :ownerId " +
            "AND (:search IS NULL OR UPPER(rc.contractNo) LIKE UPPER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR rc.status = :status)")
    Page<RentalContract> findByHouseIdWithFilters(
            @Param("houseId") Long houseId,
            @Param("ownerId") Long ownerId,
            @Param("search") String search,
            @Param("status") RentalContractStatus status,
            Pageable pageable);

    @Query("SELECT COUNT(rc) FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId = :houseId AND rc.status = :status")
    long countByHouseIdAndStatus(
            @Param("houseId") Long houseId,
            @Param("status") RentalContractStatus status);

    @Query("SELECT rc FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId = :houseId " +
            "AND (:search IS NULL OR UPPER(rc.contractNo) LIKE UPPER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR rc.status = :status)")
    Page<RentalContract> findByHouseIdWithFilters(
            @Param("houseId") Long houseId,
            @Param("search") String search,
            @Param("status") RentalContractStatus status,
            Pageable pageable);

    // ---- Owner-scoped paginated list ----

    @Query("SELECT rc FROM RentalContract rc " +
            "WHERE rc.ownerId = :ownerId " +
            "AND (:search IS NULL OR UPPER(rc.contractNo) LIKE UPPER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR rc.status = :status)")
    Page<RentalContract> findScopedByOwnerId(
            @Param("ownerId") Long ownerId,
            @Param("search") String search,
            @Param("status") RentalContractStatus status,
            Pageable pageable);

    @Query("SELECT DISTINCT rc FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId IN :houseIds " +
            "AND (:search IS NULL OR UPPER(rc.contractNo) LIKE UPPER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR rc.status = :status)")
    Page<RentalContract> findScopedByHouseIds(
            @Param("houseIds") Collection<Long> houseIds,
            @Param("search") String search,
            @Param("status") RentalContractStatus status,
            Pageable pageable);

    // ---- Invoice generation eligibility ----

    @Query(value = "SELECT DISTINCT rc FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId IN :houseIds AND rc.status = :status " +
            "AND (:search IS NULL " +
            "  OR UPPER(rc.contractNo) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "  OR UPPER(r.roomNumber) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "  OR rc.id IN (SELECT ct.contractId FROM ContractTenant ct, Tenant t " +
            "      WHERE ct.tenantId = t.id " +
            "      AND (UPPER(t.firstName) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "        OR UPPER(t.lastName) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "        OR UPPER(CONCAT(CONCAT(t.firstName, ' '), t.lastName)) " +
            "             LIKE UPPER(CONCAT('%', :search, '%'))))) " +
            "ORDER BY rc.updatedAt DESC",
            countQuery = "SELECT COUNT(DISTINCT rc) FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId IN :houseIds AND rc.status = :status " +
            "AND (:search IS NULL " +
            "  OR UPPER(rc.contractNo) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "  OR UPPER(r.roomNumber) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "  OR rc.id IN (SELECT ct.contractId FROM ContractTenant ct, Tenant t " +
            "      WHERE ct.tenantId = t.id " +
            "      AND (UPPER(t.firstName) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "        OR UPPER(t.lastName) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "        OR UPPER(CONCAT(CONCAT(t.firstName, ' '), t.lastName)) " +
            "             LIKE UPPER(CONCAT('%', :search, '%')))))")
    Page<RentalContract> findEligibleForInvoice(
            @Param("houseIds") Collection<Long> houseIds,
            @Param("search") String search,
            @Param("status") RentalContractStatus status,
            Pageable pageable);

    @Query("SELECT COUNT(DISTINCT rc) FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId IN :houseIds AND rc.status = :status")
    long countActiveByHouseIds(
            @Param("houseIds") Collection<Long> houseIds,
            @Param("status") RentalContractStatus status);

    @Query("SELECT COUNT(DISTINCT rc.id) FROM RentalContract rc, Room r, Invoice i " +
            "WHERE rc.roomId = r.id AND r.houseId IN :houseIds AND rc.status = :status " +
            "AND i.contractId = rc.id AND i.billingYear = :year AND i.billingMonth = :month")
    long countAlreadyInvoicedByHouseIds(
            @Param("houseIds") Collection<Long> houseIds,
            @Param("status") RentalContractStatus status,
            @Param("year") int year,
            @Param("month") int month);

    // ---- Summary queries (grouped counts) ----

    @Query("SELECT rc.status AS status, COUNT(rc) AS cnt FROM RentalContract rc " +
            "WHERE rc.ownerId = :ownerId GROUP BY rc.status")
    java.util.List<Object[]> countGroupByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT rc.status AS status, COUNT(rc) AS cnt FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId IN :houseIds GROUP BY rc.status")
    java.util.List<Object[]> countGroupByHouseIds(@Param("houseIds") Collection<Long> houseIds);

    @Query("SELECT COALESCE(SUM(rc.monthlyRent), 0) FROM RentalContract rc " +
            "WHERE rc.ownerId = :ownerId " +
            "AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.ACTIVE")
    BigDecimal sumActiveMonthlyRentByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT COALESCE(SUM(rc.monthlyRent), 0) FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId IN :houseIds " +
            "AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.ACTIVE")
    BigDecimal sumActiveMonthlyRentByHouseIds(@Param("houseIds") Collection<Long> houseIds);

    @Query("SELECT new com.example.spring_boot_project_api.dto.response.ExpiringContractSummary(rc.id, rc.contractNo, rc.endDate) " +
            "FROM RentalContract rc WHERE rc.ownerId = :ownerId " +
            "AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.ACTIVE " +
            "AND rc.endDate IS NOT NULL AND rc.endDate BETWEEN :start AND :end ORDER BY rc.endDate ASC")
    List<ExpiringContractSummary> findExpiringSoonByOwnerId(
            @Param("ownerId") Long ownerId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            Pageable pageable);

    @Query("SELECT new com.example.spring_boot_project_api.dto.response.ExpiringContractSummary(rc.id, rc.contractNo, rc.endDate) " +
            "FROM RentalContract rc, Room r WHERE rc.roomId = r.id AND r.houseId IN :houseIds " +
            "AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.ACTIVE " +
            "AND rc.endDate IS NOT NULL AND rc.endDate BETWEEN :start AND :end ORDER BY rc.endDate ASC")
    List<ExpiringContractSummary> findExpiringSoonByHouseIds(
            @Param("houseIds") Collection<Long> houseIds,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            Pageable pageable);

    // ---- Monthly report aggregation (house-scoped via contract -> room) ----

    @Query("""
        SELECT COUNT(rc)
        FROM RentalContract rc, Room r
        WHERE rc.roomId = r.id AND r.houseId IN :houseIds
          AND rc.createdAt >= :from AND rc.createdAt < :to
    """)
    long countCreatedByHouseIdsAndRange(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("houseIds") Collection<Long> houseIds);

    @Query("""
        SELECT COUNT(rc)
        FROM RentalContract rc, Room r
        WHERE rc.roomId = r.id AND r.houseId IN :houseIds
          AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.ACTIVE
          AND rc.startDate < :nextMonth
          AND (rc.endDate IS NULL OR rc.endDate >= :monthStart)
    """)
    long countActiveOverlappingMonthByHouseIds(@Param("monthStart") LocalDate monthStart,
            @Param("nextMonth") LocalDate nextMonth,
            @Param("houseIds") Collection<Long> houseIds);

    @Query("""
        SELECT COUNT(rc)
        FROM RentalContract rc, Room r
        WHERE rc.roomId = r.id AND r.houseId IN :houseIds
          AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.EXPIRED
          AND rc.endDate >= :monthStart AND rc.endDate < :nextMonth
    """)
    long countExpiredWithinMonthByHouseIds(@Param("monthStart") LocalDate monthStart,
            @Param("nextMonth") LocalDate nextMonth,
            @Param("houseIds") Collection<Long> houseIds);

    @Query("""
        SELECT COUNT(rc)
        FROM RentalContract rc, Room r
        WHERE rc.roomId = r.id AND r.houseId IN :houseIds
          AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.TERMINATED
          AND rc.endDate >= :monthStart AND rc.endDate < :nextMonth
    """)
    long countTerminatedWithinMonthByHouseIds(@Param("monthStart") LocalDate monthStart,
            @Param("nextMonth") LocalDate nextMonth,
            @Param("houseIds") Collection<Long> houseIds);
}
