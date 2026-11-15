package com.example.spring_boot_project_api.service.assistant.tools;

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

/** OWNER/MANAGER — page through the rooms of a single house the user can access. */
@Component
@RequiredArgsConstructor
public class GetHouseRoomsTool implements AssistantTool {

    private static final int MAX_ROOMS = 100;

    private final HouseService houseService;
    private final HouseAccessService houseAccessService;

    @Override
    public String name() {
        return "get_house_rooms";
    }

    @Override
    public String description() {
        return "List the rooms of a house with optional status and search filters.";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER);
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of(
                "houseId", "ID of the house (required)",
                "status", "Optional room status: AVAILABLE, OCCUPIED, MAINTENANCE, RESERVED",
                "roomTypeId", "Optional room type filter",
                "search", "Optional text search over room name/number");
    }

    @Override
    public AiToolResult execute(AssistantToolContext context, AssistantArguments args) {
        long houseId = args.requiredLong("houseId");
        String search = args.string("search").orElse(null);
        RoomStatus status = args.string("status")
                .map(value -> RoomStatus.valueOf(value.toUpperCase()))
                .orElse(null);
        Long roomTypeId = args.longValue("roomTypeId").orElse(null);

        houseAccessService.requireHouseAccess(houseId);

        PageResponse<RoomResponse> page = houseService.getHouseRooms(houseId, search, status,
                roomTypeId, PageRequest.of(0, MAX_ROOMS));

        return AiToolResult.success(name(), Map.of(
                "houseId", houseId,
                "total", page.totalElements(),
                "rooms", page.content()));
    }
}