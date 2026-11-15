package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.ContractRenewal;

@Repository
public interface ContractRenewalRepository extends JpaRepository<ContractRenewal, Long> {

    List<ContractRenewal> findByRentalContractIdOrderByRenewedAtDesc(Long contractId);

    @Query("""
        SELECT COUNT(cr)
        FROM ContractRenewal cr, RentalContract rc, Room r
        WHERE cr.rentalContract = rc AND rc.roomId = r.id AND r.houseId IN :houseIds
          AND cr.renewedAt >= :from AND cr.renewedAt < :to
    """)
    long countByHouseIdsAndRenewedAtRange(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, @Param("houseIds") Collection<Long> houseIds);
}
