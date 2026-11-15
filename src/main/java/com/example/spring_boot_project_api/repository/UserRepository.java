package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Searches users that can be linked to a new tenant profile, i.e. users that
     * do not already have a tenant profile. Searches across first/last/full name,
     * email, phone and username. Matching and ordering are done in the database.
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:q IS NULL OR :q = ''
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(CONCAT(TRIM(u.firstName), ' ', TRIM(u.lastName))) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')))
              AND NOT EXISTS (SELECT 1 FROM Tenant t WHERE t.user.id = u.id)
            """)
    Page<User> findAvailableForTenant(@Param("q") String q, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE (:status IS NULL OR u.status = :status)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(CONCAT(TRIM(u.firstName), ' ', TRIM(u.lastName))) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<User> findAllWithFilters(@Param("q") String q, @Param("status") UserStatus status, Pageable pageable);

    /**
     * Paginated search of users that hold the OWNER role. Optional free-text
     * search (name, full name, email, phone, username) and status filter.
     */
    @Query("""
            SELECT u FROM User u
            WHERE EXISTS (SELECT 1 FROM UserRole ur JOIN ur.role r
                          WHERE ur.user.id = u.id AND r.code = 'OWNER')
              AND (:status IS NULL OR u.status = :status)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(CONCAT(TRIM(u.firstName), ' ', TRIM(u.lastName))) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<User> findAllOwners(@Param("q") String q, @Param("status") UserStatus status, Pageable pageable);

    @Query("SELECT u.status AS status, COUNT(u) AS cnt FROM User u GROUP BY u.status")
    java.util.List<Object[]> countGroupedByStatus();

    // ------------------------------------------------------------------
    // Owner-scoped variants.
    //
    // An OWNER may only ever see:
    //   * themselves;
    //   * users linked through Tenant.user where Tenant.owner is a scoped owner
    //     or Tenant.house is an accessible house;
    //   * users of tenants on contracts (ContractTenant -> RentalContract ->
    //     Room) in an accessible house.
    //
    // Additionally excluded for an OWNER: users holding the OWNER role
    // (other than the caller) and every user holding the SUPER_ADMIN role.
    //
    // :selfId   the current user id
    // :ownerIds scoped owner ids  (HouseAccessService#getScopedOwnerIds)
    // :houseIds accessible house ids (HouseAccessService#getAccessibleHouseIds)
    // ------------------------------------------------------------------

    /** Owner-scoped counterpart of {@link #findAllWithFilters(String, UserStatus, Pageable)}. */
    @Query("""
            SELECT u FROM User u
            WHERE (
                    u.id = :selfId
                 OR EXISTS (SELECT 1 FROM Tenant t WHERE t.user.id = u.id
                            AND (t.owner.id IN :ownerIds OR t.house.id IN :houseIds))
                 OR EXISTS (SELECT 1 FROM Tenant t, ContractTenant ct, RentalContract rc, Room r
                            WHERE t.user.id = u.id AND t.id = ct.tenantId
                              AND ct.contractId = rc.id AND rc.roomId = r.id
                              AND r.houseId IN :houseIds)
                  )
              AND (u.id = :selfId
                   OR NOT EXISTS (SELECT 1 FROM UserRole ur JOIN ur.role r
                                  WHERE ur.user.id = u.id AND r.code = 'OWNER'))
              AND NOT EXISTS (SELECT 1 FROM UserRole ur JOIN ur.role r
                              WHERE ur.user.id = u.id AND r.code = 'SUPER_ADMIN')
              AND (:status IS NULL OR u.status = :status)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(CONCAT(TRIM(u.firstName), ' ', TRIM(u.lastName))) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<User> findAllInOwnerScope(@Param("q") String q,
            @Param("status") UserStatus status,
            @Param("selfId") Long selfId,
            @Param("ownerIds") Collection<Long> ownerIds,
            @Param("houseIds") Collection<Long> houseIds,
            Pageable pageable);

    /** Owner-scoped counterpart of {@link #countGroupedByStatus()}. */
    @Query("""
            SELECT u.status AS status, COUNT(u) AS cnt FROM User u
            WHERE (
                    u.id = :selfId
                 OR EXISTS (SELECT 1 FROM Tenant t WHERE t.user.id = u.id
                            AND (t.owner.id IN :ownerIds OR t.house.id IN :houseIds))
                 OR EXISTS (SELECT 1 FROM Tenant t, ContractTenant ct, RentalContract rc, Room r
                            WHERE t.user.id = u.id AND t.id = ct.tenantId
                              AND ct.contractId = rc.id AND rc.roomId = r.id
                              AND r.houseId IN :houseIds)
                  )
              AND (u.id = :selfId
                   OR NOT EXISTS (SELECT 1 FROM UserRole ur JOIN ur.role r
                                  WHERE ur.user.id = u.id AND r.code = 'OWNER'))
              AND NOT EXISTS (SELECT 1 FROM UserRole ur JOIN ur.role r
                              WHERE ur.user.id = u.id AND r.code = 'SUPER_ADMIN')
            GROUP BY u.status
            """)
    List<Object[]> countGroupedByStatusInOwnerScope(@Param("selfId") Long selfId,
            @Param("ownerIds") Collection<Long> ownerIds,
            @Param("houseIds") Collection<Long> houseIds);

    /**
     * Owner-scoped counterpart of {@link #findAvailableForTenant(String, Pageable)}.
     * The "not yet a tenant" restriction is kept on top of the ownership scope.
     */
    @Query("""
            SELECT u FROM User u
            WHERE (
                    u.id = :selfId
                 OR EXISTS (SELECT 1 FROM Tenant t WHERE t.user.id = u.id
                            AND (t.owner.id IN :ownerIds OR t.house.id IN :houseIds))
                 OR EXISTS (SELECT 1 FROM Tenant t, ContractTenant ct, RentalContract rc, Room r
                            WHERE t.user.id = u.id AND t.id = ct.tenantId
                              AND ct.contractId = rc.id AND rc.roomId = r.id
                              AND r.houseId IN :houseIds)
                  )
              AND (u.id = :selfId
                   OR NOT EXISTS (SELECT 1 FROM UserRole ur JOIN ur.role r
                                  WHERE ur.user.id = u.id AND r.code = 'OWNER'))
              AND NOT EXISTS (SELECT 1 FROM UserRole ur JOIN ur.role r
                              WHERE ur.user.id = u.id AND r.code = 'SUPER_ADMIN')
              AND NOT EXISTS (SELECT 1 FROM Tenant t2 WHERE t2.user.id = u.id)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(CONCAT(TRIM(u.firstName), ' ', TRIM(u.lastName))) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<User> findAvailableForTenantInOwnerScope(@Param("q") String q,
            @Param("selfId") Long selfId,
            @Param("ownerIds") Collection<Long> ownerIds,
            @Param("houseIds") Collection<Long> houseIds,
            Pageable pageable);

    /**
     * Per-target ownership check backing the view/update/delete authorization.
     * True only when the target user falls inside the caller's owner scope and
     * is neither the caller holding OWNER nor any SUPER_ADMIN.
     */
    @Query("""
            SELECT (COUNT(u) > 0) FROM User u
            WHERE u.id = :userId
              AND (
                    u.id = :selfId
                 OR EXISTS (SELECT 1 FROM Tenant t WHERE t.user.id = u.id
                            AND (t.owner.id IN :ownerIds OR t.house.id IN :houseIds))
                 OR EXISTS (SELECT 1 FROM Tenant t, ContractTenant ct, RentalContract rc, Room r
                            WHERE t.user.id = u.id AND t.id = ct.tenantId
                              AND ct.contractId = rc.id AND rc.roomId = r.id
                              AND r.houseId IN :houseIds)
                  )
              AND (u.id = :selfId
                   OR NOT EXISTS (SELECT 1 FROM UserRole ur JOIN ur.role r
                                  WHERE ur.user.id = u.id AND r.code = 'OWNER'))
              AND NOT EXISTS (SELECT 1 FROM UserRole ur JOIN ur.role r
                              WHERE ur.user.id = u.id AND r.code = 'SUPER_ADMIN')
            """)
    boolean existsInOwnerScope(@Param("userId") Long userId,
            @Param("selfId") Long selfId,
            @Param("ownerIds") Collection<Long> ownerIds,
            @Param("houseIds") Collection<Long> houseIds);
}
