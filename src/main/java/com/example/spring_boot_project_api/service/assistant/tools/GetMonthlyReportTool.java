package com.example.spring_boot_project_api.service.assistant.tools;

import java.time.YearMonth;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.ManagerMonthlyReportResponse;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.ManagerReportService;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantArguments;
import com.example.spring_boot_project_api.service.assistant.AssistantTool;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * OWNER/MANAGER — full monthly report (rooms, finances, contracts, payments,
 * utilities, maintenance) scoped to the user's accessible houses. The report
 * service filters houses itself; a supplied {@code houseId} is still re-checked.
 */
@Component
@RequiredArgsConstructor
public class GetMonthlyReportTool implements AssistantTool {

    private final ManagerReportService managerReportService;
    private final HouseAccessService houseAccessService;

    @Override
    public String name() {
        return "get_monthly_report";
    }

    @Override
    public String description() {
        return "Full monthly management report for a year/month with optional single-house scope.";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER);
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of(
                "year", "4-digit year (defaults to the current year)",
                "month", "Month 1-12 (defaults to the current month)",
                "houseId", "Optional single-house scope");
    }

    @Override
    public AiToolResult execute(AssistantToolContext context, AssistantArguments args) {
        YearMonth now = YearMonth.now();
        int year = args.intValue("year").orElse(now.getYear());
        int month = args.intValue("month").orElse(now.getMonthValue());
        Long houseId = args.longValue("houseId").orElse(null);

        if (houseId != null) {
            houseAccessService.requireHouseAccess(houseId);
        }

        ManagerMonthlyReportResponse report = managerReportService.getMonthlyReport(year, month, houseId);
        return AiToolResult.success(name(), report);
    }
}