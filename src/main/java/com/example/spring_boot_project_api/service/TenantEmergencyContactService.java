package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateTenantEmergencyContactRequest;
import com.example.spring_boot_project_api.dto.request.UpdateTenantEmergencyContactRequest;
import com.example.spring_boot_project_api.dto.response.TenantEmergencyContactResponse;

public interface TenantEmergencyContactService {

    TenantEmergencyContactResponse createContact(Long tenantId, CreateTenantEmergencyContactRequest request);

    List<TenantEmergencyContactResponse> getContacts(Long tenantId);

    TenantEmergencyContactResponse getContactById(Long tenantId, Long contactId);

    TenantEmergencyContactResponse updateContact(Long tenantId, Long contactId,
            UpdateTenantEmergencyContactRequest request);

    void deleteContact(Long tenantId, Long contactId);
}
