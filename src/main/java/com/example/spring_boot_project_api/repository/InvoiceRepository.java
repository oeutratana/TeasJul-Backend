package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.InvoiceItemType;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.UtilityRateStatus;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.UtilityRate;
import com.example.spring_boot_project_api.model.UtilityReading;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Invoice> findAll(Specification<Invoice> spec, Pageable pageable);

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.items WHERE i.id IN :invoiceIds")
    List<Invoice> findWithItemsByIds(@Param("invoiceIds") List<Long> invoiceIds);

    Optional<Invoice> findByIdAndOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = "items")
    List<Invoice> findByContractIdIn(List<Long> contractIds);

    @EntityGraph(attributePaths = "items")
    Page<Invoice> findByContractIdIn(List<Long> contractIds, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Optional<Invoice> findByIdAndContractIdIn(Long id, List<Long> contractIds);

    List<Invoice> findByContractIdInAndStatusIn(List<Long> contractIds, List<InvoiceStatus> statuses);

    long countByContractIdInAndStatus(List<Long> contractIds, InvoiceStatus status);

    @Query("""
        SELECT COALESCE(SUM(i.totalAmount - COALESCE(i.discount, 0)), 0)
        FROM Invoice i
        WHERE i.contractId IN :contractIds AND i.status = :status
    """)
    BigDecimal sumTotalByContractIdInAndStatus(@Param("contractIds") List<Long> contractIds,
            @Param("status") InvoiceStatus status);

    boolean existsByContractIdAndBillingYearAndBillingMonth(Long contractId, Integer billingYear, Integer billingMonth);

    @Query("SELECT DISTINCT i.contractId FROM Invoice i " +
            "WHERE i.contractId IN :contractIds " +
            "AND i.billingYear = :year AND i.billingMonth = :month")
    List<Long> findInvoicedContractIds(@Param("contractIds") Collection<Long> contractIds,
            @Param("year") int year,
            @Param("month") int month);

    Optional<Invoice> findByContractIdAndBillingYearAndBillingMonth(Long contractId, Integer billingYear, Integer billingMonth);

    // The rate effective for the given date: the latest ACTIVE rate whose
    // effective_from <= date and (effective_to is null or >= date).
    @Query("""
        SELECT r FROM UtilityRate r
        WHERE r.owner.id = :ownerId
          AND r.houseId = :houseId
          AND r.utilityTypeId = :utilityTypeId
          AND r.status = :status
          AND r.effectiveFrom <= :date
          AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date)
        ORDER BY r.effectiveFrom DESC, r.id DESC
    """)
    List<UtilityRate> findEffectiveRate(@Param("ownerId") Long ownerId,
            @Param("houseId") Long houseId,
            @Param("utilityTypeId") Long utilityTypeId,
            @Param("date") LocalDate date,
            @Param("status") UtilityRateStatus status);

    // The meter's reading for a given billing period: the latest reading on or
    // before the first day of the billing month (used as the baseline 'current'
    // for usage). Actual charge uses the reading dated within the month.
    @Query("""
        SELECT rd FROM UtilityReading rd
        WHERE rd.meter.id = :meterId
          AND rd.readingDate <= :endOfMonth
        ORDER BY rd.readingDate DESC, rd.id DESC
    """)
    List<UtilityReading> findReadingsForPeriod(@Param("meterId") Long meterId,
            @Param("endOfMonth") LocalDate endOfMonth);

    // ---- Monthly report aggregation (house-scoped via invoice -> contract -> room) ----

    @Query("""
        SELECT COALESCE(SUM(ii.amount), 0)
        FROM InvoiceItem ii, Invoice i, RentalContract c, Room r
        WHERE ii.invoice = i AND i.contractId = c.id AND c.roomId = r.id
          AND r.houseId IN :houseIds
          AND i.billingYear = :year AND i.billingMonth = :month
          AND ii.itemType = com.example.spring_boot_project_api.enums.InvoiceItemType.RENT
    """)
    BigDecimal sumRentAmountByYearMonthHouseIds(@Param("year") int year, @Param("month") int month,
            @Param("houseIds") Collection<Long> houseIds);

    @Query("""
        SELECT COALESCE(SUM(i.totalAmount - COALESCE(i.discount, 0)), 0)
        FROM Invoice i, RentalContract c, Room r
        WHERE i.contractId = c.id AND c.roomId = r.id AND r.houseId IN :houseIds
          AND i.billingYear = :year AND i.billingMonth = :month
          AND i.status IN :statuses
    """)
    BigDecimal sumOutstandingAmountByYearMonthHouseIds(@Param("year") int year, @Param("month") int month,
            @Param("houseIds") Collection<Long> houseIds,
            @Param("statuses") Collection<InvoiceStatus> statuses);

    @Query("""
        SELECT COALESCE(SUM(ii.amount), 0)
        FROM InvoiceItem ii, Invoice i, RentalContract c, Room r
        WHERE ii.invoice = i AND i.contractId = c.id AND c.roomId = r.id
          AND r.houseId IN :houseIds
          AND i.billingYear = :year AND i.billingMonth = :month
          AND ii.itemType IN :utilityTypes
    """)
    BigDecimal sumItemAmountsByYearMonthHouseIds(@Param("year") int year, @Param("month") int month,
            @Param("houseIds") Collection<Long> houseIds,
            @Param("utilityTypes") Collection<InvoiceItemType> utilityTypes);

    @Query("""
        SELECT COUNT(DISTINCT i.id)
        FROM Invoice i, RentalContract c, Room r
        WHERE i.contractId = c.id AND c.roomId = r.id AND r.houseId IN :houseIds
          AND i.billingYear = :year AND i.billingMonth = :month
          AND i.status IN :statuses
          AND EXISTS (
              SELECT 1 FROM InvoiceItem ii
              WHERE ii.invoice = i AND ii.itemType IN :utilityTypes
          )
    """)
    long countUtilityBillsByYearMonthHouseIdsAndStatusIn(@Param("year") int year, @Param("month") int month,
            @Param("houseIds") Collection<Long> houseIds,
            @Param("statuses") Collection<InvoiceStatus> statuses,
            @Param("utilityTypes") Collection<InvoiceItemType> utilityTypes);

    @Query("""
        SELECT COUNT(i.id)
        FROM Invoice i, RentalContract c, Room r
        WHERE i.contractId = c.id AND c.roomId = r.id AND r.houseId IN :houseIds
          AND i.billingYear = :year AND i.billingMonth = :month
          AND i.status = :status
    """)
    long countByYearMonthHouseIdsAndStatus(@Param("year") int year, @Param("month") int month,
            @Param("houseIds") Collection<Long> houseIds, @Param("status") InvoiceStatus status);

    @Query("""
        SELECT i.status, COUNT(i.id)
        FROM Invoice i, RentalContract c, Room r
        WHERE i.contractId = c.id AND c.roomId = r.id AND r.houseId IN :houseIds
          AND i.billingYear = :year AND i.billingMonth = :month
        GROUP BY i.status
    """)
    List<Object[]> countGroupByStatusByYearMonthHouseIds(@Param("year") int year, @Param("month") int month,
            @Param("houseIds") Collection<Long> houseIds);

    /**
     * Reads the invoices billed for a month in the given houses. Used to derive overdue
     * amounts/counts from each invoice's due date and outstanding balance, which cannot be
     * expressed as a plain status count.
     */
    @Query("""
        SELECT i FROM Invoice i, RentalContract c, Room r
        WHERE i.contractId = c.id AND c.roomId = r.id AND r.houseId IN :houseIds
          AND i.billingYear = :year AND i.billingMonth = :month
    """)
    List<Invoice> findByYearMonthHouseIds(@Param("year") int year, @Param("month") int month,
            @Param("houseIds") Collection<Long> houseIds);
}
