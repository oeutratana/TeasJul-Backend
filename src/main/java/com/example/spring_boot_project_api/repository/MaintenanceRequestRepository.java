package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.MaintenanceRequest;
import com.example.spring_boot_project_api.model.MaintenanceRequestStatus;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    Optional<MaintenanceRequest> findByIdAndOwnerId(Long id, Long ownerId);

    List<MaintenanceRequest> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<MaintenanceRequest> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<MaintenanceRequest> findAllByHouseIdInOrderByCreatedAtDesc(Collection<Long> houseIds);

    boolean existsByOwnerIdAndRequestNo(Long ownerId, String requestNo);

    long countByOwnerId(Long ownerId);

    @Query("SELECT mr.status AS status, COUNT(mr.id) AS cnt FROM MaintenanceRequest mr " +
            "WHERE mr.house.id IN :houseIds GROUP BY mr.status")
    List<Object[]> countGroupByStatusAndHouseIds(@Param("houseIds") Collection<Long> houseIds);

    long countByHouseIdIn(Collection<Long> houseIds);

    @Query("""
        SELECT mr.status, COUNT(mr.id)
        FROM MaintenanceRequest mr
        WHERE mr.house.id IN :houseIds
          AND mr.reportedAt >= :from AND mr.reportedAt < :to
        GROUP BY mr.status
    """)
    List<Object[]> countGroupByStatusAndHouseIdsAndReportedAtRange(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, @Param("houseIds") Collection<Long> houseIds);
}
