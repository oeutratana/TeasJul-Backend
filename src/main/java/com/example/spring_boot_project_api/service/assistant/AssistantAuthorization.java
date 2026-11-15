package com.example.spring_boot_project_api.service.assistant;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * Role-based access control for the assistant. A user holding several roles
 * receives the union of the tools allowed by each role; a role block must never
 * widen any single role's scope.
 */
@Component
@RequiredArgsConstructor
public class AssistantAuthorization {

    /** Text returned when a tool (or the question behind it) is out of the user's permission scope. */
    public static final String REFUSAL = "I'm sorry, you don't have permission to access this information.";

    private static final List<String> KNOWN_ROLES =
            List.of(SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER, SecurityUtils.ROLE_TENANT);

    private final SecurityUtils securityUtils;

    public Set<String> roleCodesOf(Long userId) {
        return KNOWN_ROLES.stream()
                .filter(role -> securityUtils.hasRole(userId, role))
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<String> permittedToolNames(List<AssistantTool> tools, Set<String> roleCodes) {
        return tools.stream()
                .filter(tool -> overlaps(tool.allowedRoles(), roleCodes))
                .map(AssistantTool::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean overlaps(Set<String> allowedRoles, Set<String> roleCodes) {
        for (String role : allowedRoles) {
            if (roleCodes.contains(role)) {
                return true;
            }
        }
        return false;
    }
}