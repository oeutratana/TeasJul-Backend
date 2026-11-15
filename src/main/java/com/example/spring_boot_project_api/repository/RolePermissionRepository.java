package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.RolePermission;
import com.example.spring_boot_project_api.model.RolePermissionId;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);

    List<RolePermission> findByRoleId(Long roleId);

    boolean existsByRoleIdInAndPermission_Code(Collection<Long> roleIds, String code);

    void deleteByRoleId(Long roleId);

    void deleteByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
