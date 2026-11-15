package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreateTenantEmergencyContactRequest;
import com.example.spring_boot_project_api.dto.request.UpdateTenantEmergencyContactRequest;
import com.example.spring_boot_project_api.dto.response.TenantEmergencyContactResponse;
import com.example.spring_boot_project_api.service.TenantEmergencyContactService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/emergency-contacts")
@RequiredArgsConstructor
@Tag(name = "Tenant Emergency Contacts",
        description = "Emergency contact management endpoints (tenant ownership is verified against the authenticated user)")
public class TenantEmergencyContactController {

    private final TenantEmergencyContactService contactService;

    @Operation(summary = "Create an emergency contact",
            description = "Creates an emergency contact for the tenant if the tenant belongs to the currently authenticated user.")
    @PostMapping
    public ResponseEntity<TenantEmergencyContactResponse> createContact(@PathVariable Long tenantId,
            @Valid @RequestBody CreateTenantEmergencyContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contactService.createContact(tenantId, request));
    }

    @Operation(summary = "List emergency contacts",
            description = "Returns all emergency contacts of the tenant if the tenant belongs to the currently authenticated user.")
    @GetMapping
    public ResponseEntity<List<TenantEmergencyContactResponse>> getContacts(@PathVariable Long tenantId) {
        return ResponseEntity.ok(contactService.getContacts(tenantId));
    }

    @Operation(summary = "Get an emergency contact by id",
            description = "Returns the emergency contact with the given id if it belongs to the tenant "
                    + "and the tenant belongs to the currently authenticated user.")
    @GetMapping("/{contactId}")
    public ResponseEntity<TenantEmergencyContactResponse> getContactById(@PathVariable Long tenantId,
            @PathVariable Long contactId) {
        return ResponseEntity.ok(contactService.getContactById(tenantId, contactId));
    }

    @Operation(summary = "Update an emergency contact",
            description = "Updates the emergency contact with the given id if it belongs to the tenant "
                    + "and the tenant belongs to the currently authenticated user. Only provided fields are updated.")
    @PutMapping("/{contactId}")
    public ResponseEntity<TenantEmergencyContactResponse> updateContact(@PathVariable Long tenantId,
            @PathVariable Long contactId, @Valid @RequestBody UpdateTenantEmergencyContactRequest request) {
        return ResponseEntity.ok(contactService.updateContact(tenantId, contactId, request));
    }

    @Operation(summary = "Delete an emergency contact",
            description = "Deletes the emergency contact with the given id if it belongs to the tenant "
                    + "and the tenant belongs to the currently authenticated user.")
    @DeleteMapping("/{contactId}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long tenantId, @PathVariable Long contactId) {
        contactService.deleteContact(tenantId, contactId);
        return ResponseEntity.noContent().build();
    }
}
