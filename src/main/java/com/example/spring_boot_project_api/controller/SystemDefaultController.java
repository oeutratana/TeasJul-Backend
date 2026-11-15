package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.SystemDefaultViewResponse;
import com.example.spring_boot_project_api.enums.SystemDefaultKind;
import com.example.spring_boot_project_api.service.SystemDefaultService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Owner-facing system default selection. The owner is resolved from the
 * authenticated user and authorization is enforced in
 * {@link SystemDefaultService}.
 */
@RestController
@RequestMapping("/api/system-defaults")
@RequiredArgsConstructor
@Tag(name = "System Defaults",
        description = "Owner-facing system default utilities &amp; amenities "
                + "(enable/disable/list for the authenticated owner).")
public class SystemDefaultController {

    private final SystemDefaultService systemDefaultService;

    @Operation(summary = "List system defaults for the current owner",
            description = "Returns active system defaults with the current "
                    + "owner's assignment state, so the owner page can separate "
                    + "System Defaults, System-Enabled and Owner Custom items.")
    @GetMapping
    public ResponseEntity<List<SystemDefaultViewResponse>> listForOwner(
            @RequestParam(required = false) SystemDefaultKind kind) {
        return ResponseEntity.ok(systemDefaultService.listDefaultsForOwner(kind));
    }

    @Operation(summary = "Enable a system default for the current owner",
            description = "Materialises (or reuses by code/name) the owner-scoped "
                    + "utility type or amenity for the given system default and "
                    + "marks it enabled for the current owner.")
    @PostMapping("/{id}/enable")
    public ResponseEntity<SystemDefaultViewResponse> enableForOwner(@PathVariable Long id) {
        return ResponseEntity.ok(systemDefaultService.enableForOwner(id));
    }

    @Operation(summary = "Disable a system default for the current owner",
            description = "Marks the assignment of the given system default as "
                    + "disabled for the current owner. Existing owner data "
                    + "(meters, rates, rooms) is preserved.")
    @PostMapping("/{id}/disable")
    public ResponseEntity<SystemDefaultViewResponse> disableForOwner(@PathVariable Long id) {
        return ResponseEntity.ok(systemDefaultService.disableForOwner(id));
    }
}