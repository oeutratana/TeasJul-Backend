package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByOwnerId(Long ownerId);

    Page<Expense> findByOwnerId(Long ownerId, Pageable pageable);

    Optional<Expense> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndExpenseNo(Long ownerId, String expenseNo);

    boolean existsByOwnerIdAndExpenseNoAndIdNot(Long ownerId, String expenseNo, Long id);
}
