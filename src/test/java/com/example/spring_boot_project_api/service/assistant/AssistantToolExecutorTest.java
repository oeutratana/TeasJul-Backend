package com.example.spring_boot_project_api.service.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.response.HouseSummaryResponse;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.HouseService;
import com.example.spring_boot_project_api.service.assistant.tools.GetHouseSummaryTool;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssistantToolExecutorTest {

    @Mock
    private HouseService houseService;
    @Mock
    private HouseAccessService houseAccessService;

    @Test
    void executesToolForAllowedRoleAndReturnsData() {
        GetHouseSummaryTool tool = new GetHouseSummaryTool(houseService, houseAccessService);
        when(houseService.getHouseSummary(12L))
                .thenReturn(new HouseSummaryResponse(10, 4, 5, 1, 3, 2));

        AiToolResult result = executorOf(tool).execute("get_house_summary",
                context(SecurityUtils.ROLE_OWNER), Map.of("houseId", "12"));

        assertThat(result.success()).isTrue();
        assertThat(result.toolName()).isEqualTo("get_house_summary");
        assertThat(((HouseSummaryResponse) result.data()).totalRooms()).isEqualTo(10);
        verify(houseAccessService).requireHouseAccess(12L);
        verify(houseService).getHouseSummary(12L);
        verifyNoMoreInteractions(houseService);
    }

    @Test
    void deniesToolForDisallowedRoleEvenWhenRoleAllowsNothing() {
        GetHouseSummaryTool tool = new GetHouseSummaryTool(houseService, houseAccessService);

        AiToolResult result = executorOf(tool).execute("get_house_summary",
                context(SecurityUtils.ROLE_TENANT), Map.of("houseId", "12"));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).isEqualTo(AssistantAuthorization.REFUSAL);
        verifyNoInteractions(houseService);
        verifyNoInteractions(houseAccessService);
    }

    @Test
    void deniesHouseOutsideScope() {
        GetHouseSummaryTool tool = new GetHouseSummaryTool(houseService, houseAccessService);
        doThrow(new AccessDeniedException("No access"))
                .when(houseAccessService).requireHouseAccess(99L);

        AiToolResult result = executorOf(tool).execute("get_house_summary",
                context(SecurityUtils.ROLE_OWNER), Map.of("houseId", "99"));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).isEqualTo(AssistantAuthorization.REFUSAL);
        verify(houseService, org.mockito.Mockito.never()).getHouseSummary(any());
    }

    @Test
    void rejectsMissingRequiredArgument() {
        GetHouseSummaryTool tool = new GetHouseSummaryTool(houseService, houseAccessService);

        AiToolResult result = executorOf(tool).execute("get_house_summary",
                context(SecurityUtils.ROLE_OWNER), Map.of());

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("houseId");
    }

    @Test
    void unknownToolIsAcknowledgedAsFailure() {
        AiToolResult result = executorOf().execute("no_such_tool",
                context(SecurityUtils.ROLE_OWNER), Map.of());

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("Unknown tool");
    }

    @Test
    void runtimeExceptionsFromServicesAreCapturedAsFailure() {
        GetHouseSummaryTool tool = new GetHouseSummaryTool(houseService, houseAccessService);
        when(houseService.getHouseSummary(12L)).thenThrow(new IllegalStateException("boom"));

        AiToolResult result = executorOf(tool).execute("get_house_summary",
                context(SecurityUtils.ROLE_OWNER), Map.of("houseId", "12"));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).isEqualTo("boom");
    }

    private AssistantToolExecutor executorOf(AssistantTool... tools) {
        return new AssistantToolExecutor(List.of(tools));
    }

    private AssistantToolContext context(String... roles) {
        return new AssistantToolContext(1L, Set.of(roles));
    }
}