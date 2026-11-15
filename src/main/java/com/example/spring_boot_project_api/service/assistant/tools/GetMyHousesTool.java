package com.example.spring_boot_project_api.service.assistant.tools;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.service.HouseService;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantArguments;
import com.example.spring_boot_project_api.service.assistant.AssistantTool;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/** OWNER/MANAGER — list the houses owned by or assigned to the user. */
@Component
@RequiredArgsConstructor
public class GetMyHousesTool implements AssistantTool {

    private final HouseService houseService;

    @Override
    public String name() {
        return "get_my_houses";
    }

    @Override
    public String description() {
        return "List the rental properties owned by or assigned to the current user.";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER);
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of();
    }

    @Override
    public AiToolResult execute(AssistantToolContext context, AssistantArguments args) {
        List<HouseResponse> houses = houseService.getMyHouses();
        return AiToolResult.success(name(), houses);
    }
}