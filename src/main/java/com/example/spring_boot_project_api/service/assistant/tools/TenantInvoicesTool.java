package com.example.spring_boot_project_api.service.assistant.tools;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceResponse;
import com.example.spring_boot_project_api.service.TenantDashboardService;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantArguments;
import com.example.spring_boot_project_api.service.assistant.AssistantTool;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.service.impl.TenantContextResolver;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/** TENANT — the tenant's own invoices; resolved from the authenticated user. */
@Component
@RequiredArgsConstructor
public class TenantInvoicesTool implements AssistantTool {

    private static final int MAX_INVOICES = 100;

    private final TenantContextResolver tenantContextResolver;
    private final TenantDashboardService tenantDashboardService;

    @Override
    public String name() {
        return "tenant_invoices";
    }

    @Override
    public String description() {
        return "List my invoices with amounts per utility and payment status.";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(SecurityUtils.ROLE_TENANT);
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of(
                "page", "Optional zero-based page number (default 0)",
                "size", "Optional page size (capped at " + MAX_INVOICES + ")");
    }

    @Override
    public AiToolResult execute(AssistantToolContext context, AssistantArguments args) {
        tenantContextResolver.requireLinkedTenant(context.userId());

        int page = Math.max(args.intValue("page").orElse(0), 0);
        int size = Math.min(args.intValue("size").orElse(20), MAX_INVOICES);

        PageResponse<TenantInvoiceResponse> invoices = tenantDashboardService.getMyInvoices(page, size);
        return AiToolResult.success(name(), Map.of(
                "total", invoices.totalElements(),
                "invoices", invoices.content()));
    }
}