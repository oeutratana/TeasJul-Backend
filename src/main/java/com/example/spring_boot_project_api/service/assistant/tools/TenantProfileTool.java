package com.example.spring_boot_project_api.service.assistant.tools;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.TenantMeResponse;
import com.example.spring_boot_project_api.service.TenantPortalService;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantArguments;
import com.example.spring_boot_project_api.service.assistant.AssistantTool;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.service.impl.TenantContextResolver;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/** TENANT — the tenant's own profile, room and current contract. */
@Component
@RequiredArgsConstructor
public class TenantProfileTool implements AssistantTool {

    private final TenantContextResolver tenantContextResolver;
    private final TenantPortalService tenantPortalService;

    @Override
    public String name() {
        return "tenant_profile";
    }

    @Override
    public String description() {
        return "Show my tenant profile, current room and current rental contract.";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(SecurityUtils.ROLE_TENANT);
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of();
    }

    @Override
    public AiToolResult execute(AssistantToolContext context, AssistantArguments args) {
        tenantContextResolver.requireLinkedTenant(context.userId());

        TenantMeResponse profile = tenantPortalService.getMyTenantProfile();
        return AiToolResult.success(name(), profile);
    }
}