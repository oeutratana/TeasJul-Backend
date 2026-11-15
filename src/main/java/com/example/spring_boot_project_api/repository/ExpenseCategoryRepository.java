package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.ExpenseCategory;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    List<ExpenseCategory> findByOwnerId(Long ownerId);

    Page<ExpenseCategory> findByOwnerId(Long ownerId, Pageable pageable);

    Optional<ExpenseCategory> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndCode(Long ownerId, String code);

    boolean existsByOwnerIdAndName(Long ownerId, String name);

    boolean existsByOwnerIdAndCodeAndIdNot(Long ownerId, String code, Long id);

    boolean existsByOwnerIdAndNameAndIdNot(Long ownerId, String name, Long id);
}
