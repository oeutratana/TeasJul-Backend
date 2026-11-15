package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findWithLockById(@Param("id") Long id);

    List<Room> findByHouseId(Long houseId);

    List<Room> findByHouseIdIn(List<Long> houseIds);

    List<Room> findByOwnerId(Long ownerId);

    boolean existsByOwnerRecordId(Long ownerRecordId);

    List<Room> findByRoomTypeId(Long roomTypeId);

    Optional<Room> findByHouseIdAndRoomNumber(Long houseId, String roomNumber);

    boolean existsByHouseIdAndRoomNumber(Long houseId, String roomNumber);

    Page<Room> findByPublicationStatus(RoomPublicationStatus publicationStatus, Pageable pageable);

    /**
     * Admin property-posting list: every {@link Room} as seen by the
     * SUPER_ADMIN posting flow, optionally filtered by publication status,
     * property type and a search term matching the room, its house or its
     * room number.
     */
    @Query("""
            SELECT r FROM Room r
            WHERE (:status IS NULL OR r.publicationStatus = :status)
              AND (:type IS NULL OR r.propertyType = :type)
              AND (:search IS NULL
                   OR UPPER(COALESCE(r.name, '')) LIKE UPPER(CONCAT('%', :search, '%'))
                   OR UPPER(COALESCE(r.roomNumber, '')) LIKE UPPER(CONCAT('%', :search, '%'))
                   OR EXISTS (SELECT 1 FROM House h WHERE h.id = r.houseId
                              AND UPPER(h.name) LIKE UPPER(CONCAT('%', :search, '%'))))
            ORDER BY r.updatedAt DESC, r.id DESC
            """)
    Page<Room> findAdminProperties(
            @Param("search") String search,
            @Param("type") PropertyType type,
            @Param("status") RoomPublicationStatus status,
            Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.publicationStatus = :publicationStatus " +
            "ORDER BY r.submittedAt ASC, r.id ASC")
    Page<Room> findPendingApprovalRooms(@Param("publicationStatus") RoomPublicationStatus publicationStatus,
            Pageable pageable);

    /**
     * SUPER_ADMIN review-workflow listing: every {@link Room}, optionally
     * filtered by publication status and/or room availability status. Both
     * filters are independent and nullable, so a room that is PUBLISHED while
     * OCCUPIED is still returned (and vice versa). Callers apply the sort via
     * the {@link Pageable}.
     */
    @Query("""
            SELECT r FROM Room r
            WHERE (:publicationStatus IS NULL OR r.publicationStatus = :publicationStatus)
              AND (:roomStatus IS NULL OR r.status = :roomStatus)
            """)
    Page<Room> findRoomsForReview(
            @Param("publicationStatus") RoomPublicationStatus publicationStatus,
            @Param("roomStatus") RoomStatus roomStatus,
            Pageable pageable);

    Page<Room> findByPublicationStatusAndStatus(RoomPublicationStatus publicationStatus,
            RoomStatus roomStatus, Pageable pageable);

    Optional<Room> findByIdAndPublicationStatusAndStatus(Long id, RoomPublicationStatus publicationStatus,
            RoomStatus roomStatus);

    List<Room> findByHouseIdAndRoomTypeIdAndPublicationStatusAndStatusOrderByFloorNumberAscRoomNumberAscIdAsc(
            Long houseId, Long roomTypeId, RoomPublicationStatus publicationStatus,
            RoomStatus roomStatus);

    /**
     * Public browsing query: published and available rooms, optionally filtered by
     * search term (room number/name), minimum rent and maximum rent.
     */
    @Query("SELECT r FROM Room r "
            + "WHERE r.publicationStatus = :publicationStatus AND r.status = :roomStatus "
            + "AND (:search IS NULL OR UPPER(r.roomNumber) LIKE UPPER(CONCAT('%', :search, '%')) "
            + "OR UPPER(r.name) LIKE UPPER(CONCAT('%', :search, '%'))) "
            + "AND (:minRent IS NULL OR r.monthlyRent >= :minRent) "
            + "AND (:maxRent IS NULL OR r.monthlyRent <= :maxRent)")
    Page<Room> findPublishedWithFilters(
            @Param("publicationStatus") RoomPublicationStatus publicationStatus,
            @Param("roomStatus") RoomStatus roomStatus,
            @Param("search") String search,
            @Param("minRent") BigDecimal minRent,
            @Param("maxRent") BigDecimal maxRent,
            Pageable pageable);

    /**
     * Public listing groups with search and filters. Each row represents one
     * House + RoomType pair and includes only rooms visible to anonymous visitors
     * matching the filter criteria.
     */
    @Query(value = """
        SELECT MIN(r.id) AS representativeRoomId,
               r.houseId AS houseId,
               r.roomTypeId AS roomTypeId,
               COUNT(r.id) AS availableRoomCount,
               MIN(r.monthlyRent) AS fromRent
        FROM Room r
        JOIN House h ON r.houseId = h.id
        LEFT JOIN RoomType rt ON r.roomTypeId = rt.id
        WHERE r.publicationStatus = :publicationStatus
          AND r.status = :roomStatus
          AND (:search IS NULL OR UPPER(r.roomNumber) LIKE UPPER(CONCAT('%', :search, '%'))
               OR UPPER(r.name) LIKE UPPER(CONCAT('%', :search, '%'))
               OR UPPER(h.name) LIKE UPPER(CONCAT('%', :search, '%'))
               OR UPPER(h.address) LIKE UPPER(CONCAT('%', :search, '%'))
               OR UPPER(rt.name) LIKE UPPER(CONCAT('%', :search, '%')))
          AND (:cityProvince IS NULL OR UPPER(h.cityProvince) LIKE UPPER(CONCAT('%', :cityProvince, '%')))
          AND (:propertyType IS NULL OR r.propertyType = :propertyType OR (r.propertyType IS NULL AND h.propertyType = :propertyType))
          AND (:roomTypeId IS NULL OR r.roomTypeId = :roomTypeId)
          AND (:listingScope IS NULL OR r.listingScope = :listingScope)
          AND (:furnishing IS NULL OR r.furnishing = :furnishing OR (r.furnishing IS NULL AND h.furnishing = :furnishing))
          AND (:minRent IS NULL OR r.monthlyRent >= :minRent)
          AND (:maxRent IS NULL OR r.monthlyRent <= :maxRent)
          AND (:bedrooms IS NULL OR COALESCE(r.bedrooms, h.bedrooms) = :bedrooms)
          AND (:bathrooms IS NULL OR COALESCE(r.bathrooms, h.bathrooms) = :bathrooms)
          AND (:minAreaSqm IS NULL OR COALESCE(r.areaSqm, h.areaSqm) >= :minAreaSqm)
          AND (:maxAreaSqm IS NULL OR COALESCE(r.areaSqm, h.areaSqm) <= :maxAreaSqm)
          AND (:capacity IS NULL OR r.capacity >= :capacity)
          AND (:availableFrom IS NULL OR COALESCE(r.availableFrom, h.availableFrom) IS NULL OR COALESCE(r.availableFrom, h.availableFrom) <= :availableFrom)
          AND (:hasAmenities = FALSE OR r.id IN (
              SELECT ra.id.roomId FROM RoomAmenity ra JOIN ra.amenity a
              WHERE UPPER(a.name) IN :amenityList OR UPPER(a.code) IN :amenityList OR CAST(a.id AS string) IN :amenityList
          ))
          AND (:hasUtilities = FALSE OR r.id IN (
              SELECT ru.roomId FROM RoomUtility ru JOIN UtilityType ut ON ru.utilityTypeId = ut.id
              WHERE ru.status = com.example.spring_boot_project_api.enums.UtilityTypeStatus.ACTIVE
                AND (UPPER(ut.name) IN :utilityList OR UPPER(ut.code) IN :utilityList OR CAST(ut.id AS string) IN :utilityList)
          ))
        GROUP BY r.houseId, r.roomTypeId
        ORDER BY
          CASE WHEN :sortOrder = 'PRICE_ASC' THEN MIN(r.monthlyRent) END ASC,
          CASE WHEN :sortOrder = 'PRICE_DESC' THEN MIN(r.monthlyRent) END DESC,
          CASE WHEN :sortOrder <> 'PRICE_ASC' AND :sortOrder <> 'PRICE_DESC' THEN MAX(r.publishedAt) END DESC,
          MIN(r.id) ASC
    """, countQuery = """
        SELECT COUNT(r)
        FROM Room r JOIN House h ON r.houseId = h.id
        WHERE r.id IN (
            SELECT MIN(r2.id)
            FROM Room r2 JOIN House h2 ON r2.houseId = h2.id
            LEFT JOIN RoomType rt2 ON r2.roomTypeId = rt2.id
            WHERE r2.publicationStatus = :publicationStatus
              AND r2.status = :roomStatus
              AND (:search IS NULL OR UPPER(r2.roomNumber) LIKE UPPER(CONCAT('%', :search, '%'))
                   OR UPPER(r2.name) LIKE UPPER(CONCAT('%', :search, '%'))
                   OR UPPER(h2.name) LIKE UPPER(CONCAT('%', :search, '%'))
                   OR UPPER(h2.address) LIKE UPPER(CONCAT('%', :search, '%'))
                   OR UPPER(rt2.name) LIKE UPPER(CONCAT('%', :search, '%')))
              AND (:cityProvince IS NULL OR UPPER(h2.cityProvince) LIKE UPPER(CONCAT('%', :cityProvince, '%')))
              AND (:propertyType IS NULL OR r2.propertyType = :propertyType OR (r2.propertyType IS NULL AND h2.propertyType = :propertyType))
              AND (:roomTypeId IS NULL OR r2.roomTypeId = :roomTypeId)
              AND (:listingScope IS NULL OR r2.listingScope = :listingScope)
              AND (:furnishing IS NULL OR r2.furnishing = :furnishing OR (r2.furnishing IS NULL AND h2.furnishing = :furnishing))
              AND (:minRent IS NULL OR r2.monthlyRent >= :minRent)
              AND (:maxRent IS NULL OR r2.monthlyRent <= :maxRent)
              AND (:bedrooms IS NULL OR COALESCE(r2.bedrooms, h2.bedrooms) = :bedrooms)
              AND (:bathrooms IS NULL OR COALESCE(r2.bathrooms, h2.bathrooms) = :bathrooms)
              AND (:minAreaSqm IS NULL OR COALESCE(r2.areaSqm, h2.areaSqm) >= :minAreaSqm)
              AND (:maxAreaSqm IS NULL OR COALESCE(r2.areaSqm, h2.areaSqm) <= :maxAreaSqm)
              AND (:capacity IS NULL OR r2.capacity >= :capacity)
              AND (:availableFrom IS NULL OR COALESCE(r2.availableFrom, h2.availableFrom) IS NULL OR COALESCE(r2.availableFrom, h2.availableFrom) <= :availableFrom)
              AND (:hasAmenities = FALSE OR r2.id IN (
                  SELECT ra2.id.roomId FROM RoomAmenity ra2 JOIN ra2.amenity a2
                  WHERE UPPER(a2.name) IN :amenityList OR UPPER(a2.code) IN :amenityList OR CAST(a2.id AS string) IN :amenityList
              ))
              AND (:hasUtilities = FALSE OR r2.id IN (
                  SELECT ru2.roomId FROM RoomUtility ru2 JOIN UtilityType ut2 ON ru2.utilityTypeId = ut2.id
                  WHERE ru2.status = com.example.spring_boot_project_api.enums.UtilityTypeStatus.ACTIVE
                    AND (UPPER(ut2.name) IN :utilityList OR UPPER(ut2.code) IN :utilityList OR CAST(ut2.id AS string) IN :utilityList)
              ))
            GROUP BY r2.houseId, r2.roomTypeId
        )
    """)
    Page<PublicRoomGroupProjection> findPublicRoomGroups(
            @Param("publicationStatus") RoomPublicationStatus publicationStatus,
            @Param("roomStatus") RoomStatus roomStatus,
            @Param("search") String search,
            @Param("cityProvince") String cityProvince,
            @Param("propertyType") PropertyType propertyType,
            @Param("roomTypeId") Long roomTypeId,
            @Param("listingScope") ListingScope listingScope,
            @Param("furnishing") Furnishing furnishing,
            @Param("minRent") BigDecimal minRent,
            @Param("maxRent") BigDecimal maxRent,
            @Param("bedrooms") Integer bedrooms,
            @Param("bathrooms") Integer bathrooms,
            @Param("minAreaSqm") BigDecimal minAreaSqm,
            @Param("maxAreaSqm") BigDecimal maxAreaSqm,
            @Param("capacity") Integer capacity,
            @Param("availableFrom") LocalDate availableFrom,
            @Param("hasAmenities") boolean hasAmenities,
            @Param("amenityList") List<String> amenityList,
            @Param("hasUtilities") boolean hasUtilities,
            @Param("utilityList") List<String> utilityList,
            @Param("sortOrder") String sortOrder,
            Pageable pageable);

    @Query("SELECT DISTINCT r.roomTypeId FROM Room r "
            + "WHERE r.roomTypeId IS NOT NULL "
            + "AND r.publicationStatus = :publicationStatus AND r.status = :roomStatus")
    List<Long> findDistinctPublicRoomTypeIds(
            @Param("publicationStatus") RoomPublicationStatus publicationStatus,
            @Param("roomStatus") RoomStatus roomStatus);

    Optional<Room> findByIdAndPublicationStatus(Long id,
            RoomPublicationStatus publicationStatus);

    Page<Room> findByOwnerIdAndPublicationStatus(Long ownerId,
            RoomPublicationStatus publicationStatus, Pageable pageable);

    Page<Room> findByOwnerIdInAndPublicationStatus(Collection<Long> ownerIds,
            RoomPublicationStatus publicationStatus, Pageable pageable);

    long countByHouseIdAndStatus(Long houseId, RoomStatus status);

    long countByHouseId(Long houseId);

    @Query("SELECT r.houseId AS houseId, COUNT(r) AS roomCount FROM Room r "
            + "WHERE r.houseId IN :houseIds GROUP BY r.houseId")
    List<HouseRoomCountProjection> countRoomsByHouseIds(@Param("houseIds") Collection<Long> houseIds);

    @Query("SELECT r.houseId AS houseId, COUNT(r) AS roomCount FROM Room r "
            + "WHERE r.houseId IN :houseIds AND r.status = :status GROUP BY r.houseId")
    List<HouseRoomCountProjection> countRoomsByHouseIdsAndStatus(
            @Param("houseIds") Collection<Long> houseIds,
            @Param("status") RoomStatus status);

    long countByHouseIdIn(Collection<Long> houseIds);

    long countByHouseIdInAndStatus(Collection<Long> houseIds, RoomStatus status);

    @Query("SELECT r FROM Room r WHERE r.houseId = :houseId AND r.ownerId = :ownerId " +
            "AND (:search IS NULL OR UPPER(r.roomNumber) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "OR UPPER(r.name) LIKE UPPER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:roomTypeId IS NULL OR r.roomTypeId = :roomTypeId)")
    Page<Room> findByHouseIdWithFilters(
            @Param("houseId") Long houseId,
            @Param("ownerId") Long ownerId,
            @Param("search") String search,
            @Param("status") RoomStatus status,
            @Param("roomTypeId") Long roomTypeId,
            Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.houseId = :houseId " +
            "AND (:search IS NULL OR UPPER(r.roomNumber) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "OR UPPER(r.name) LIKE UPPER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:roomTypeId IS NULL OR r.roomTypeId = :roomTypeId)")
    Page<Room> findByHouseIdWithFilters(
            @Param("houseId") Long houseId,
            @Param("search") String search,
            @Param("status") RoomStatus status,
            @Param("roomTypeId") Long roomTypeId,
            Pageable pageable);

    List<Room> findByListingIdOrderByRoomNumberAsc(Long listingId);

    List<Room> findByListingId(Long listingId);

    List<Room> findByListingIdAndStatusOrderByRoomNumberAsc(Long listingId, RoomStatus status);

    long countByListingId(Long listingId);

    List<Room> findByHouseIdAndRoomTypeIdAndStatusAndListingIdIsNull(Long houseId,
            Long roomTypeId, RoomStatus status);

    List<Room> findByHouseIdAndRoomTypeIdAndStatusAndListingId(Long houseId,
            Long roomTypeId, RoomStatus status, Long listingId);

    @Query("SELECT MIN(r.id) AS representativeRoomId, " +
            "r.houseId AS houseId, " +
            "r.roomTypeId AS roomTypeId, " +
            "COUNT(r.id) AS availableRoomCount, " +
            "MIN(r.monthlyRent) AS fromRent " +
            "FROM Room r " +
            "WHERE (r.houseId <> :houseId OR r.roomTypeId <> :roomTypeId) " +
            "AND r.roomTypeId IS NOT NULL " +
            "AND r.publicationStatus = :publicationStatus AND r.status = :status " +
            "GROUP BY r.houseId, r.roomTypeId " +
            "ORDER BY ABS(MIN(r.monthlyRent) - :monthlyRent) ASC, MIN(r.id) ASC")
    List<PublicRoomGroupProjection> findSimilarRoomGroups(
             @Param("houseId") Long houseId,
             @Param("roomTypeId") Long roomTypeId,
             @Param("monthlyRent") BigDecimal monthlyRent,
             @Param("publicationStatus") RoomPublicationStatus publicationStatus,
             @Param("status") RoomStatus status,
             Pageable pageable);
}
