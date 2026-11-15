package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.MaintenanceCost;

@Repository
public interface MaintenanceCostRepository extends JpaRepository<MaintenanceCost, Long> {

    List<MaintenanceCost> findAllByRequestIdOrderByCreatedAtAsc(Long requestId);

    Optional<MaintenanceCost> findByIdAndRequestId(Long id, Long requestId);

    @Query("""
        SELECT COALESCE(SUM(mc.totalAmount), 0)
        FROM MaintenanceCost mc JOIN mc.request mr
        WHERE mr.house.id IN :houseIds
          AND mc.createdAt >= :from AND mc.createdAt < :to
    """)
    BigDecimal sumTotalAmountByHouseIdsAndCreatedAtRange(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, @Param("houseIds") Collection<Long> houseIds);
}
