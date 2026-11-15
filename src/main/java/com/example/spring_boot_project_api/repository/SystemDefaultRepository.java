package com.example.spring_boot_project_api.repository;

import com.example.spring_boot_project_api.enums.SystemDefaultKind;
import com.example.spring_boot_project_api.enums.SystemDefaultStatus;
import com.example.spring_boot_project_api.model.SystemDefault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemDefaultRepository extends JpaRepository<SystemDefault, Long> {
    List<SystemDefault> findByKindOrderByNameAsc(SystemDefaultKind kind);

    List<SystemDefault> findByKindAndStatusOrderByNameAsc(SystemDefaultKind kind, SystemDefaultStatus status);

    Optional<SystemDefault> findByKindAndCode(SystemDefaultKind kind, String code);

    boolean existsByKindAndCode(SystemDefaultKind kind, String code);

    boolean existsByKindAndCodeAndIdNot(SystemDefaultKind kind, String code, Long id);

    boolean existsByKindAndName(SystemDefaultKind kind, String name);

    boolean existsByKindAndNameAndIdNot(SystemDefaultKind kind, String name, Long id);
}