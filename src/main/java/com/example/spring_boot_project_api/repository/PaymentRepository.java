package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    @Override
    @EntityGraph(attributePaths = "paymentMethod")
    Page<Payment> findAll(Specification<Payment> spec, Pageable pageable);

    Optional<Payment> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<Payment> findByPaymentNo(String paymentNo);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByInvoiceIdAndStatusIn(Long invoiceId, List<PaymentStatus> statuses);

    List<Payment> findByInvoiceIdInAndStatus(List<Long> invoiceIds, PaymentStatus status);

    List<Payment> findByInvoiceIdIn(List<Long> invoiceIds);

    @Query("SELECT p.invoiceId, COALESCE(SUM(p.amount), 0) FROM Payment p "
            + "WHERE p.invoiceId IN :invoiceIds AND p.status = :status GROUP BY p.invoiceId")
    List<Object[]> sumPaidAmountByInvoiceIds(@Param("invoiceIds") List<Long> invoiceIds,
            @Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoiceId = :invoiceId AND p.status = 'PAID'")
    BigDecimal findPaidTotalByInvoiceId(@Param("invoiceId") Long invoiceId);

    long count();

    // ---- Monthly report aggregation (house-scoped via payment -> invoice -> contract -> room) ----

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p, Invoice i, RentalContract c, Room r
        WHERE p.invoiceId = i.id AND i.contractId = c.id AND c.roomId = r.id
          AND r.houseId IN :houseIds
          AND p.status = com.example.spring_boot_project_api.enums.PaymentStatus.PAID
          AND p.paidAt >= :from AND p.paidAt < :to
    """)
    BigDecimal sumPaidAmountByHouseIdsAndPaidAtRange(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, @Param("houseIds") Collection<Long> houseIds);

    @Query("""
        SELECT COUNT(p.id)
        FROM Payment p, Invoice i, RentalContract c, Room r
        WHERE p.invoiceId = i.id AND i.contractId = c.id AND c.roomId = r.id
          AND r.houseId IN :houseIds
          AND p.status = :status
          AND p.paidAt >= :from AND p.paidAt < :to
    """)
    long countByHouseIdsAndStatusAndPaidAtRange(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, @Param("houseIds") Collection<Long> houseIds,
            @Param("status") PaymentStatus status);

    @Query("""
        SELECT COUNT(p.id)
        FROM Payment p, Invoice i, RentalContract c, Room r
        WHERE p.invoiceId = i.id AND i.contractId = c.id AND c.roomId = r.id
          AND r.houseId IN :houseIds
          AND p.status = :status
          AND p.createdAt >= :from AND p.createdAt < :to
    """)
    long countByHouseIdsAndStatusAndCreatedAtRange(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, @Param("houseIds") Collection<Long> houseIds,
            @Param("status") PaymentStatus status);

    @Query("""
        SELECT FUNCTION('YEAR', p.paidAt), FUNCTION('MONTH', p.paidAt), COALESCE(SUM(p.amount), 0)
        FROM Payment p, Invoice i, RentalContract c, Room r
        WHERE p.invoiceId = i.id AND i.contractId = c.id AND c.roomId = r.id
          AND r.houseId IN :houseIds
          AND p.status = com.example.spring_boot_project_api.enums.PaymentStatus.PAID
          AND p.paidAt >= :from AND p.paidAt < :to
        GROUP BY FUNCTION('YEAR', p.paidAt), FUNCTION('MONTH', p.paidAt)
    """)
    List<Object[]> sumPaidGroupByYearMonthByHouseIds(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, @Param("houseIds") Collection<Long> houseIds);
}
