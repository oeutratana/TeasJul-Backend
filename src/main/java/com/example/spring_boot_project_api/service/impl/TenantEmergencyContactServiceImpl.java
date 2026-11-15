package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateTenantEmergencyContactRequest;
import com.example.spring_boot_project_api.dto.request.UpdateTenantEmergencyContactRequest;
import com.example.spring_boot_project_api.dto.response.TenantEmergencyContactResponse;
import com.example.spring_boot_project_api.exception.TenantEmergencyContactNotFoundException;
import com.example.spring_boot_project_api.exception.TenantNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.TenantEmergencyContactMapper;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantEmergencyContact;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.TenantEmergencyContactRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.TenantEmergencyContactService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantEmergencyContactServiceImpl implements TenantEmergencyContactService {

    private final TenantEmergencyContactRepository contactRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantEmergencyContactMapper contactMapper;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional
    public TenantEmergencyContactResponse createContact(Long tenantId,
            CreateTenantEmergencyContactRequest request) {
        Tenant tenant = getOwnedTenant(tenantId);

        TenantEmergencyContact contact = contactMapper.toEntity(request);
        contact.setTenant(tenant);

        return contactMapper.toResponse(contactRepository.save(contact));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantEmergencyContactResponse> getContacts(Long tenantId) {
        getOwnedTenant(tenantId);

        return contactRepository.findAllByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(contactMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantEmergencyContactResponse getContactById(Long tenantId, Long contactId) {
        return contactMapper.toResponse(getOwnedContact(tenantId, contactId));
    }

    @Override
    @Transactional
    public TenantEmergencyContactResponse updateContact(Long tenantId, Long contactId,
            UpdateTenantEmergencyContactRequest request) {
        TenantEmergencyContact contact = getOwnedContact(tenantId, contactId);

        if (request.name() != null) {
            contact.setName(request.name());
        }
        if (request.relationship() != null) {
            contact.setRelationship(request.relationship());
        }
        if (request.phone() != null) {
            contact.setPhone(request.phone());
        }
        if (request.address() != null) {
            contact.setAddress(request.address());
        }
        if (request.isPrimary() != null) {
            contact.setIsPrimary(request.isPrimary());
        }

        return contactMapper.toResponse(contactRepository.save(contact));
    }

    @Override
    @Transactional
    public void deleteContact(Long tenantId, Long contactId) {
        TenantEmergencyContact contact = getOwnedContact(tenantId, contactId);
        contactRepository.delete(contact);
    }

    private Tenant getOwnedTenant(Long tenantId) {
        Long currentUserId = getCurrentOwner().getId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> TenantNotFoundException.forId(tenantId));

        boolean isOwner = tenant.getOwner().getId().equals(currentUserId);
        if (isOwner) {
            return tenant;
        }

        if (tenant.getHouse() != null && houseAccessService.canAccessHouse(tenant.getHouse().getId())) {
            return tenant;
        }

        throw TenantNotFoundException.forId(tenantId);
    }

    private TenantEmergencyContact getOwnedContact(Long tenantId, Long contactId) {
        getOwnedTenant(tenantId);

        return contactRepository.findByIdAndTenantId(contactId, tenantId)
                .orElseThrow(() -> TenantEmergencyContactNotFoundException.forId(contactId));
    }

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
