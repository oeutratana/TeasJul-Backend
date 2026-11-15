package com.example.spring_boot_project_api.service.assistant.tools;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.HouseSummaryResponse;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.HouseService;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantArguments;
import com.example.spring_boot_project_api.service.assistant.AssistantTool;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * OWNER/MANAGER — occupancy/financial summary for a single house. Access is
 * re-checked through {@link HouseAccessService} before the existing service is
 * called.
 */
@Component
@RequiredArgsConstructor
public class GetHouseSummaryTool implements AssistantTool {

    private final HouseService houseService;
    private final HouseAccessService houseAccessService;

    @Override
    public String name() {
        return "get_house_summary";
    }

    @Override
    public String description() {
        return "Summarize a single house: total, available, occupied and maintenance rooms plus tenant/contract counts.";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER);
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of("houseId", "ID of the house to summarize (required)");
    }

    @Override
    public AiToolResult execute(AssistantToolContext context, AssistantArguments args) {
        long houseId = args.requiredLong("houseId");

        houseAccessService.requireHouseAccess(houseId);

        HouseSummaryResponse summary = houseService.getHouseSummary(houseId);
        return AiToolResult.success(name(), summary);
    }
}