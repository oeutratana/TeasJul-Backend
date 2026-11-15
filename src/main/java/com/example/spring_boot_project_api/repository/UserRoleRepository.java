package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserRole;
import com.example.spring_boot_project_api.model.UserRoleId;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    @Query("SELECT (COUNT(ur) > 0) FROM UserRole ur " +
            "JOIN ur.role r WHERE ur.user.id = :userId AND r.code = :code")
    boolean existsByUserIdAndRoleCode(@Param("userId") Long userId, @Param("code") String code);

    @Query("SELECT ur.user FROM UserRole ur " +
            "JOIN ur.role r WHERE r.code = :code")
    java.util.List<User> findUsersByRoleCode(@Param("code") String code);

    @Query("SELECT ur.role.id FROM UserRole ur WHERE ur.user.id = :userId")
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    List<UserRole> findByUserId(Long userId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}
