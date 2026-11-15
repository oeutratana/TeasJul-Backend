package com.example.spring_boot_project_api.service;

import java.util.List;

/**
 * Centralized house-level authorization service.
 * All house-scoped access decisions must flow through this service.
 *
 * <p>OWNER access rule: {@code house.owner_id == currentUserId}
 * <p>MANAGER access rule: {@code user_house_access.user_id == currentUserId AND user_house_access.house_id == requestedHouseId}
 * <p>If neither condition is true → 403 FORBIDDEN
 */
public interface HouseAccessService {

    /** Returns true if the current user may read the given house. */
    boolean canAccessHouse(Long houseId);

    /** Returns true if the current user may write/manage the given house (owner or manager with access). */
    boolean canManageHouse(Long houseId);

    /** Throws AccessDeniedException if the current user may not read the given house. */
    void requireHouseAccess(Long houseId);

    /** Throws AccessDeniedException if the current user is not the owner of the given house. */
    void requireOwnerHouseAccess(Long houseId);

    /** Returns the houseId that the given room belongs to, after verifying the current user has access. */
    Long resolveRoomHouseId(Long roomId);

    /** Returns the houseId that the given contract belongs to (via room), after verifying access. */
    Long resolveContractHouseId(Long contractId);

    /** Returns the houseId that the given invoice belongs to (via contract → room), after verifying access. */
    Long resolveInvoiceHouseId(Long invoiceId);

    /** Returns the houseId that the given tenant belongs to, after verifying access. */
    Long resolveTenantHouseId(Long tenantId);

    /** Returns all house IDs accessible to the current user. */
    List<Long> getAccessibleHouseIds();

    /** Returns all owner IDs reachable through the current user's house access (for manager scoping). */
    List<Long> getScopedOwnerIds();

    /** Returns true if the current user has the OWNER role. */
    boolean isOwner();

    /** Returns true if the current user has the MANAGER role. */
    boolean isManager();
}
