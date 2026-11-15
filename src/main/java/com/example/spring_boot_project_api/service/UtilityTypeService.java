package com.example.spring_boot_project_api.service;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.CreateUtilityTypeRequest;
import com.example.spring_boot_project_api.dto.request.UpdateUtilityTypeRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityTypeResponse;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;

public interface UtilityTypeService {

    UtilityTypeResponse createType(CreateUtilityTypeRequest request);

    PageResponse<UtilityTypeResponse> getTypes(String search, UtilityTypeStatus status, Pageable pageable);

    UtilityTypeResponse getTypeById(Long id);

    UtilityTypeResponse updateType(Long id, UpdateUtilityTypeRequest request);

    void deactivateType(Long id);
}
