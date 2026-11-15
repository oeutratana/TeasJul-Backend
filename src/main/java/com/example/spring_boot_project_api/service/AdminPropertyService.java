package com.example.spring_boot_project_api.service;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.AdminPropertyPostRequest;
import com.example.spring_boot_project_api.dto.request.PropertyOwnerRequest;
import com.example.spring_boot_project_api.dto.response.AdminCatalogResponse;
import com.example.spring_boot_project_api.dto.response.AdminPropertyResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.PropertyOwnerResponse;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;

/**
 * ADMIN/SUPER_ADMIN property-posting workflow. This is intentionally separate
 * from the OWNER portal: an admin posts a complete property (house / villa /
 * apartment / condo / room) on behalf of an owner who may or may not have a
 * TeasJul login account.
 *
 * <p>Every operation requires the SUPER_ADMIN role (checked in the
 * implementation with an explicit role check, never the generic permission
 * check which auto-grants OWNER).</p>
 */
public interface AdminPropertyService {

    PageResponse<AdminPropertyResponse> list(String search, PropertyType propertyType,
            RoomPublicationStatus status, Pageable pageable);

    AdminPropertyResponse get(Long roomId);

    AdminPropertyResponse create(AdminPropertyPostRequest request);

    AdminPropertyResponse update(Long roomId, AdminPropertyPostRequest request);

    AdminCatalogResponse getCatalog();

    PageResponse<PropertyOwnerResponse> listPropertyOwners(String search, Pageable pageable);

    PropertyOwnerResponse createPropertyOwner(PropertyOwnerRequest request);

    void deletePropertyOwner(Long id);
}