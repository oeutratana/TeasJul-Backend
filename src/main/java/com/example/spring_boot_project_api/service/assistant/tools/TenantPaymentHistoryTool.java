package com.example.spring_boot_project_api.service.assistant.tools;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.TenantPaymentHistoryResponse;
import com.example.spring_boot_project_api.service.TenantDashboardService;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantArguments;
import com.example.spring_boot_project_api.service.assistant.AssistantTool;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.service.impl.TenantContextResolver;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/** TENANT — per-month payment history for my own invoices. */
@Component
@RequiredArgsConstructor
public class TenantPaymentHistoryTool implements AssistantTool {

    private final TenantContextResolver tenantContextResolver;
    private final TenantDashboardService tenantDashboardService;

    @Override
    public String name() {
        return "tenant_payment_history";
    }

    @Override
    public String description() {
        return "Show what I paid in previous months (per-month totals and status).";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(SecurityUtils.ROLE_TENANT);
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of(
                "from", "Optional start month (yyyy-MM)",
                "to", "Optional end month (yyyy-MM)");
    }

    @Override
    public AiToolResult execute(AssistantToolContext context, AssistantArguments args) {
        tenantContextResolver.requireLinkedTenant(context.userId());

        YearMonth from = args.yearMonth("from").orElse(null);
        YearMonth to = args.yearMonth("to").orElse(null);

        List<TenantPaymentHistoryResponse> history = tenantDashboardService.getPaymentHistory(from, to);
        return AiToolResult.success(name(), history);
    }
}