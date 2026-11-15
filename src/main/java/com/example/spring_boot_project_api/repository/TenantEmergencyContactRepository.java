package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.TenantEmergencyContact;

@Repository
public interface TenantEmergencyContactRepository extends JpaRepository<TenantEmergencyContact, Long> {

    List<TenantEmergencyContact> findAllByTenantIdOrderByCreatedAtAsc(Long tenantId);

    Optional<TenantEmergencyContact> findByIdAndTenantId(Long id, Long tenantId);

    void deleteByTenantId(Long tenantId);
}
