package com.example.spring_boot_project_api.service.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssistantAuthorizationTest {

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AssistantAuthorization authorization;

    private AssistantTool tool(String name, String... roles) {
        return new AssistantTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return name + " description";
            }

            @Override
            public Set<String> allowedRoles() {
                return Set.of(roles);
            }

            @Override
            public Map<String, String> parameters() {
                return Map.of();
            }

            @Override
            public AiToolResult execute(AssistantToolContext context, AssistantArguments args) {
                return AiToolResult.success(name(), null);
            }
        };
    }

    @Test
    void roleCodesOf_collectsOnlyAssignedRoles() {
        when(securityUtils.hasRole(eq(7L), eq(SecurityUtils.ROLE_OWNER))).thenReturn(true);

        assertThat(authorization.roleCodesOf(7L))
                .containsExactlyInAnyOrder(SecurityUtils.ROLE_OWNER);
    }

    @Test
    void roleCodesOf_collectsUnionForMultiRoleUser() {
        when(securityUtils.hasRole(eq(7L), eq(SecurityUtils.ROLE_OWNER))).thenReturn(true);
        when(securityUtils.hasRole(eq(7L), eq(SecurityUtils.ROLE_TENANT))).thenReturn(true);

        assertThat(authorization.roleCodesOf(7L)).containsExactlyInAnyOrder(
                SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_TENANT);
    }

    @Test
    void roleCodesOf_returnsEmptyWhenNoRoleAssigned() {
        assertThat(authorization.roleCodesOf(7L)).isEmpty();
    }

    @Test
    void permittedToolNames_ownerReceivesOnlyAdminTools() {
        List<AssistantTool> tools = List.of(
                tool("get_house_summary", SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER),
                tool("tenant_balance", SecurityUtils.ROLE_TENANT));

        assertThat(authorization.permittedToolNames(tools, Set.of(SecurityUtils.ROLE_OWNER)))
                .containsExactly("get_house_summary");
    }

    @Test
    void permittedToolNames_tenantCannotUseOwnerManagerTools() {
        List<AssistantTool> tools = List.of(
                tool("get_house_summary", SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER),
                tool("get_vacant_rooms", SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER),
                tool("tenant_balance", SecurityUtils.ROLE_TENANT));

        assertThat(authorization.permittedToolNames(tools, Set.of(SecurityUtils.ROLE_TENANT)))
                .containsExactly("tenant_balance");
    }

    @Test
    void permittedToolNames_multiRoleUserReceivesUnionOfTools() {
        List<AssistantTool> tools = List.of(
                tool("get_house_summary", SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER),
                tool("tenant_balance", SecurityUtils.ROLE_TENANT),
                tool("tenant_invoices", SecurityUtils.ROLE_TENANT));

        assertThat(authorization.permittedToolNames(tools,
                Set.of(SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_TENANT)))
                .containsExactlyInAnyOrder(
                        "get_house_summary", "tenant_balance", "tenant_invoices");
    }
}