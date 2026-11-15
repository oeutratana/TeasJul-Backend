package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.PaymentMethodStatus;
import com.example.spring_boot_project_api.model.PaymentMethod;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    Optional<PaymentMethod> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<PaymentMethod> findFirstByOwnerIdAndStatus(Long ownerId, PaymentMethodStatus status);

    Optional<PaymentMethod> findFirstByOwnerIdAndCodeAndStatus(Long ownerId, String code, PaymentMethodStatus status);

    boolean existsByOwnerIdAndCodeAndStatus(Long ownerId, String code, PaymentMethodStatus status);

    boolean existsByOwnerIdAndCode(Long ownerId, String code);

    List<PaymentMethod> findByOwnerIdInOrderByIdAsc(List<Long> ownerIds);
}
