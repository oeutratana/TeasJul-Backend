package com.example.spring_boot_project_api.service.assistant.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.HouseService;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantArguments;
import com.example.spring_boot_project_api.service.assistant.AssistantTool;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * OWNER/MANAGER — vacant ({@link RoomStatus#AVAILABLE}) rooms across the user's
 * accessible houses, or a single house when {@code houseId} is given. Uses only
 * existing services; never touches repositories.
 */
@Component
@RequiredArgsConstructor
public class GetVacantRoomsTool implements AssistantTool {

    private static final int MAX_ROOMS = 100;

    private final HouseService houseService;
    private final HouseAccessService houseAccessService;

    @Override
    public String name() {
        return "get_vacant_rooms";
    }

    @Override
    public String description() {
        return "List available (vacant) rooms across the user's houses with their rent.";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER);
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of("houseId", "Optional single-house scope");
    }

    @Override
    public AiToolResult execute(AssistantToolContext context, AssistantArguments args) {
        List<Long> houseIds;
        Long houseId = args.longValue("houseId").orElse(null);
        if (houseId != null) {
            houseAccessService.requireHouseAccess(houseId);
            houseIds = List.of(houseId);
        } else {
            houseIds = houseAccessService.getAccessibleHouseIds();
        }

        List<Map<String, Object>> byHouse = new ArrayList<>();
        long totalVacant = 0;
        for (Long accessibleHouseId : houseIds) {
            PageResponse<RoomResponse> page = houseService.getHouseRooms(accessibleHouseId, null,
                    RoomStatus.AVAILABLE, null, PageRequest.of(0, MAX_ROOMS));
            totalVacant += page.totalElements();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("houseId", accessibleHouseId);
            entry.put("total", page.totalElements());
            entry.put("rooms", page.content());
            byHouse.add(entry);
        }

        return AiToolResult.success(name(), Map.of(
                "totalVacantRooms", totalVacant,
                "houses", byHouse));
    }
}