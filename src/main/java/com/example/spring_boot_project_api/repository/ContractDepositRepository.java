package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.ContractDepositStatus;

import com.example.spring_boot_project_api.model.ContractDeposit;

@Repository
public interface ContractDepositRepository extends JpaRepository<ContractDeposit, Long> {

    List<ContractDeposit> findByRentalContractId(Long contractId);

    List<ContractDeposit> findByStatus(ContractDepositStatus status);

    @Query("""
        SELECT COALESCE(SUM(d.paidAmount), 0)
        FROM ContractDeposit d, RentalContract rc, Room r
        WHERE d.rentalContract = rc AND rc.roomId = r.id AND r.houseId IN :houseIds
          AND d.status = com.example.spring_boot_project_api.enums.ContractDepositStatus.PAID
    """)
    BigDecimal sumPaidAmountByHouseIds(@Param("houseIds") Collection<Long> houseIds);

    @Query("""
        SELECT COALESCE(SUM(d.paidAmount), 0)
        FROM ContractDeposit d, RentalContract rc, Room r
        WHERE d.rentalContract = rc AND rc.roomId = r.id AND r.houseId IN :houseIds
          AND d.status = com.example.spring_boot_project_api.enums.ContractDepositStatus.PAID
          AND d.paidAt >= :from AND d.paidAt < :to
    """)
    BigDecimal sumPaidAmountByHouseIdsAndPaidAtRange(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, @Param("houseIds") Collection<Long> houseIds);
}
