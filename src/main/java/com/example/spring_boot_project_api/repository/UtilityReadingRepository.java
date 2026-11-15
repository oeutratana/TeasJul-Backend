package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.UtilityReading;

@Repository
public interface UtilityReadingRepository
        extends JpaRepository<UtilityReading, Long>, JpaSpecificationExecutor<UtilityReading> {

    Optional<UtilityReading> findTopByMeterIdOrderByReadingDateDesc(Long meterId);

    List<UtilityReading> findByMeterIdInAndReadingDateBetween(List<Long> meterIds, LocalDate start, LocalDate end);

    Optional<UtilityReading> findTopByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(Long meterId, LocalDate date);

    Optional<UtilityReading> findTopByMeterIdAndReadingDateBetweenOrderByReadingDateDesc(
            Long meterId, LocalDate start, LocalDate end);

    boolean existsByMeterIdAndReadingDate(Long meterId, LocalDate readingDate);

    @Query("""
        SELECT ut.code, COALESCE(SUM(r.usage), 0)
        FROM UtilityReading r, UtilityMeter m, UtilityType ut
        WHERE r.meter = m AND ut.id = m.utilityTypeId
          AND m.houseId IN :houseIds
          AND r.readingDate >= :from AND r.readingDate < :to
        GROUP BY ut.code
    """)
    List<Object[]> sumUsageGroupByTypeCodeByHouseIdsAndDateRange(@Param("from") LocalDate from,
            @Param("to") LocalDate to, @Param("houseIds") Collection<Long> houseIds);
}
