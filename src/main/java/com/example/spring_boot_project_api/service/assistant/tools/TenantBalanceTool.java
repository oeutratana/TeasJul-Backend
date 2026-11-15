package com.example.spring_boot_project_api.service.assistant.tools;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse;
import com.example.spring_boot_project_api.service.TenantDashboardService;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantArguments;
import com.example.spring_boot_project_api.service.assistant.AssistantTool;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.service.impl.TenantContextResolver;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/** TENANT — current amount due and payment summary for the authenticated tenant. */
@Component
@RequiredArgsConstructor
public class TenantBalanceTool implements AssistantTool {

    private final TenantContextResolver tenantContextResolver;
    private final TenantDashboardService tenantDashboardService;

    @Override
    public String name() {
        return "tenant_balance";
    }

    @Override
    public String description() {
        return "Show how much I currently owe and my payment summary.";
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

        TenantDashboardResponse dashboard = tenantDashboardService.getDashboard();
        return AiToolResult.success(name(), dashboard.financial());
    }
}