package com.example.spring_boot_project_api.service.assistant.tools;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.ManagerDashboardResponse;
import com.example.spring_boot_project_api.service.ManagerDashboardService;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantArguments;
import com.example.spring_boot_project_api.service.assistant.AssistantTool;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * OWNER/MANAGER — dashboard KPIs and trend charts for the user's accessible
 * houses. The existing service scopes the data and validates single-house
 * access itself.
 */
@Component
@RequiredArgsConstructor
public class GetDashboardSummaryTool implements AssistantTool {

    private final ManagerDashboardService managerDashboardService;

    @Override
    public String name() {
        return "get_dashboard_summary";
    }

    @Override
    public String description() {
        return "Dashboard KPIs, revenue trend and occupancy/payment/maintenance breakdown.";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER);
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of(
                "houseId", "Optional single-house scope",
                "from", "Optional inclusive start date (yyyy-MM-dd)",
                "to", "Optional inclusive end date (yyyy-MM-dd)");
    }

    @Override
    public AiToolResult execute(AssistantToolContext context, AssistantArguments args) {
        Long houseId = args.longValue("houseId").orElse(null);
        LocalDate from = args.localDate("from").orElse(null);
        LocalDate to = args.localDate("to").orElse(null);

        ManagerDashboardResponse dashboard =
                managerDashboardService.getDashboard(houseId, from, to, null, null);
        return AiToolResult.success(name(), dashboard);
    }
}