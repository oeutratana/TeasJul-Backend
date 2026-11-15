package com.example.spring_boot_project_api.service.assistant.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse;
import com.example.spring_boot_project_api.dto.response.TenantUtilityTrendResponse;
import com.example.spring_boot_project_api.exception.TenantNotFoundException;
import com.example.spring_boot_project_api.service.TenantDashboardService;
import com.example.spring_boot_project_api.service.TenantPortalService;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantArguments;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.service.impl.TenantContextResolver;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssistantTenantToolSecurityTest {

    private static final Long TENANT_USER_ID = 3L;

    @Mock
    private TenantContextResolver tenantContextResolver;
    @Mock
    private TenantPortalService tenantPortalService;
    @Mock
    private TenantDashboardService tenantDashboardService;

    private static final AssistantToolContext TENANT =
            new AssistantToolContext(TENANT_USER_ID, Set.of(SecurityUtils.ROLE_TENANT));

    @BeforeEach
    void setUp() {
        when(tenantContextResolver.requireLinkedTenant(TENANT_USER_ID)).thenReturn(null);
    }

    @Test
    void tenantProfile_resolvesTenantFromAuthenticationAndNeverFromArgs() {
        when(tenantPortalService.getMyTenantProfile()).thenReturn(null);

        AiToolResult result = new TenantProfileTool(tenantContextResolver, tenantPortalService)
                .execute(TENANT, args("tenantId", "999"));

        assertThat(result.success()).isTrue();
        verify(tenantContextResolver).requireLinkedTenant(TENANT_USER_ID);
        verify(tenantPortalService).getMyTenantProfile();
        verifyNoMoreInteractions(tenantPortalService);
    }

    @Test
    void tenantInvoices_resolvesTenantAndIgnoresForgedTenantIdArg() {
        when(tenantDashboardService.getMyInvoices(0, 20))
                .thenReturn(PageResponse.of(List.of(), 0, 20, 0, 0));

        AiToolResult result = new TenantInvoicesTool(tenantContextResolver, tenantDashboardService)
                .execute(TENANT, args("tenantId", "999"));

        assertThat(result.success()).isTrue();
        verify(tenantContextResolver).requireLinkedTenant(TENANT_USER_ID);
        verify(tenantDashboardService).getMyInvoices(0, 20);
    }

    @Test
    void tenantBalance_returnsFinancialSectionOfOwnDashboard() {
        TenantDashboardResponse dashboard = new TenantDashboardResponse(null, null, null,
                new TenantDashboardResponse.TenantDashboardFinancial(
                        new BigDecimal("120.00"), new BigDecimal("80.00"), 1, 1, 0),
                null);
        when(tenantDashboardService.getDashboard()).thenReturn(dashboard);

        AiToolResult result = new TenantBalanceTool(tenantContextResolver, tenantDashboardService)
                .execute(TENANT, args());

        assertThat(result.success()).isTrue();
        assertThat(((TenantDashboardResponse.TenantDashboardFinancial) result.data()).currentDue())
                .isEqualByComparingTo("120.00");
        verify(tenantContextResolver).requireLinkedTenant(TENANT_USER_ID);
    }

    @Test
    void tenantUtilityTrend_passesOptionalMonthRange() {
        when(tenantDashboardService.getUtilityUsage(YearMonth.of(2026, 1), YearMonth.of(2026, 3)))
                .thenReturn(new TenantUtilityTrendResponse(List.of(), List.of()));

        AiToolResult result = new TenantUtilityTrendTool(tenantContextResolver, tenantDashboardService)
                .execute(TENANT, args("from", "2026-01", "to", "2026-03"));

        assertThat(result.success()).isTrue();
        verify(tenantDashboardService).getUtilityUsage(YearMonth.of(2026, 1), YearMonth.of(2026, 3));
    }

    @Test
    void tenantUtilityTrend_defaultsToNullRangeWhenNotSupplied() {
        when(tenantDashboardService.getUtilityUsage(isNull(), isNull()))
                .thenReturn(new TenantUtilityTrendResponse(List.of(), List.of()));

        AiToolResult result = new TenantUtilityTrendTool(tenantContextResolver, tenantDashboardService)
                .execute(TENANT, args());

        assertThat(result.success()).isTrue();
        verify(tenantDashboardService).getUtilityUsage(isNull(), isNull());
    }

    @Test
    void tenantPaymentHistory_defaultsToNullRangeWhenNotSupplied() {
        when(tenantDashboardService.getPaymentHistory(isNull(), isNull())).thenReturn(List.of());

        AiToolResult result = new TenantPaymentHistoryTool(tenantContextResolver,
                tenantDashboardService).execute(TENANT, args());

        assertThat(result.success()).isTrue();
        verify(tenantContextResolver).requireLinkedTenant(TENANT_USER_ID);
        verify(tenantDashboardService).getPaymentHistory(isNull(), isNull());
    }

    @Test
    void tenantTools_neverExposeAnotherTenantsIdentifierAsParameter() {
        assertThat(new TenantInvoicesTool(tenantContextResolver, tenantDashboardService)
                .parameters().keySet()).doesNotContain("tenantId");
        assertThat(new TenantBalanceTool(tenantContextResolver, tenantDashboardService)
                .parameters().keySet()).doesNotContain("tenantId");
        assertThat(new TenantUtilityTrendTool(tenantContextResolver, tenantDashboardService)
                .parameters().keySet()).doesNotContain("tenantId");
        assertThat(new TenantPaymentHistoryTool(tenantContextResolver, tenantDashboardService)
                .parameters().keySet()).doesNotContain("tenantId");
    }

    @Test
    void tenantWithoutLinkedProfile_isRejectedWithClearMessage() {
        org.mockito.Mockito.reset(tenantContextResolver);
        when(tenantContextResolver.requireLinkedTenant(eq(3L)))
                .thenThrow(new TenantNotFoundException(
                        "No tenant profile is linked to your account."));

        assertThatThrownBy(() -> new TenantInvoicesTool(tenantContextResolver, tenantDashboardService)
                .execute(TENANT, args()))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessageContaining("No tenant profile is linked");
    }

    private static AssistantArguments args(String... pairs) {
        java.util.LinkedHashMap<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return new AssistantArguments(map);
    }
}