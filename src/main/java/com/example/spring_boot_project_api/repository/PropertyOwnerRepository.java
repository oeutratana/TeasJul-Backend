package com.example.spring_boot_project_api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.PropertyOwner;

@Repository
public interface PropertyOwnerRepository extends JpaRepository<PropertyOwner, Long> {

    Page<PropertyOwner> findByNameContainingIgnoreCase(String name, Pageable pageable);
}