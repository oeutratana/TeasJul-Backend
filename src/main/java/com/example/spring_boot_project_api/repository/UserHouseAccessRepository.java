package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.UserHouseAccess;
import com.example.spring_boot_project_api.model.UserHouseAccessId;

@Repository
public interface UserHouseAccessRepository extends JpaRepository<UserHouseAccess, UserHouseAccessId> {

    boolean existsByUserIdAndHouseId(Long userId, Long houseId);

    List<UserHouseAccess> findByUserId(Long userId);

    List<UserHouseAccess> findByHouseId(Long houseId);

    @Query("SELECT a.house FROM UserHouseAccess a WHERE a.user.id = :userId ORDER BY a.house.createdAt DESC")
    List<House> findHousesByUserId(@Param("userId") Long userId);

    @Query("SELECT a.house.id FROM UserHouseAccess a WHERE a.user.id = :userId")
    List<Long> findHouseIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT a.house.owner.id FROM UserHouseAccess a WHERE a.user.id = :userId")
    List<Long> findOwnerIdsByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndHouseId(Long userId, Long houseId);
}
