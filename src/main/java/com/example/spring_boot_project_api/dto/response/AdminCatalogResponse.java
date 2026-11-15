package com.example.spring_boot_project_api.dto.response;

import java.util.List;

/**
 * Reference data for the admin property-posting wizard: available amenities,
 * utility types, non-login property-owner records and TeasJul OWNER accounts.
 * Any of the two owner kinds can be attached to a new post.
 */
public record AdminCatalogResponse(
        List<AmenityOption> amenities,
        List<UtilityTypeOption> utilityTypes,
        List<PropertyOwnerResponse> propertyOwners,
        List<UserResponse> ownerAccounts) {

    public record AmenityOption(Long id, String name, String code) {
    }

    public record UtilityTypeOption(Long id, String name, String unit) {
    }
}