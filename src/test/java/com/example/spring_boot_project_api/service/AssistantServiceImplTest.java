package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.config.AiProperties;
import com.example.spring_boot_project_api.config.MockChatClient;
import com.example.spring_boot_project_api.dto.request.AssistantChatRequest;
import com.example.spring_boot_project_api.dto.response.AssistantChatResponse;
import com.example.spring_boot_project_api.dto.response.HouseSummaryResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.AssistantDisabledException;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantAuthorization;
import com.example.spring_boot_project_api.service.assistant.AssistantToolExecutor;
import com.example.spring_boot_project_api.service.assistant.dto.AiToolCall;
import com.example.spring_boot_project_api.service.assistant.tools.GetDashboardSummaryTool;
import com.example.spring_boot_project_api.service.assistant.tools.GetHouseRoomsTool;
import com.example.spring_boot_project_api.service.assistant.tools.GetHouseSummaryTool;
import com.example.spring_boot_project_api.service.assistant.tools.GetInvoiceTool;
import com.example.spring_boot_project_api.service.assistant.tools.GetInvoicesTool;
import com.example.spring_boot_project_api.service.assistant.tools.GetMonthlyReportTool;
import com.example.spring_boot_project_api.service.assistant.tools.GetMyHousesTool;
import com.example.spring_boot_project_api.service.assistant.tools.GetOverdueInvoicesTool;
import com.example.spring_boot_project_api.service.assistant.tools.GetVacantRoomsTool;
import com.example.spring_boot_project_api.service.assistant.tools.TenantBalanceTool;
import com.example.spring_boot_project_api.service.assistant.tools.TenantInvoicesTool;
import com.example.spring_boot_project_api.service.assistant.tools.TenantPaymentHistoryTool;
import com.example.spring_boot_project_api.service.assistant.tools.TenantProfileTool;
import com.example.spring_boot_project_api.service.assistant.tools.TenantUtilityTrendTool;
import com.example.spring_boot_project_api.service.impl.AssistantServiceImpl;
import com.example.spring_boot_project_api.service.impl.TenantContextResolver;
import com.example.spring_boot_project_api.util.SecurityUtils;

/**
 * Full assistant pipeline with the real deterministic {@link MockChatClient}:
 * role denials, multi-role unions and house-access denials are all exercised
 * through the actual message-to-tool routing.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssistantServiceImplTest {

    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private HouseService houseService;
    @Mock
    private HouseAccessService houseAccessService;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private ManagerReportService managerReportService;
    @Mock
    private ManagerDashboardService managerDashboardService;
    @Mock
    private TenantPortalService tenantPortalService;
    @Mock
    private TenantDashboardService tenantDashboardService;
    @Mock
    private TenantContextResolver tenantContextResolver;

    private AiProperties aiProperties;
    private AssistantServiceImpl service;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        aiProperties.setEnabled(true);

        AssistantToolExecutor executor = new AssistantToolExecutor(List.of(
                new GetMyHousesTool(houseService),
                new GetHouseSummaryTool(houseService, houseAccessService),
                new GetHouseRoomsTool(houseService, houseAccessService),
                new GetMonthlyReportTool(managerReportService, houseAccessService),
                new GetInvoicesTool(invoiceService, houseAccessService),
                new GetInvoiceTool(invoiceService),
                new GetDashboardSummaryTool(managerDashboardService),
                new GetVacantRoomsTool(houseService, houseAccessService),
                new GetOverdueInvoicesTool(invoiceService, houseAccessService),
                new TenantProfileTool(tenantContextResolver, tenantPortalService),
                new TenantInvoicesTool(tenantContextResolver, tenantDashboardService),
                new TenantBalanceTool(tenantContextResolver, tenantDashboardService),
                new TenantUtilityTrendTool(tenantContextResolver, tenantDashboardService),
                new TenantPaymentHistoryTool(tenantContextResolver, tenantDashboardService)));

        service = new AssistantServiceImpl(aiProperties, new MockChatClient(), executor,
                new AssistantAuthorization(securityUtils), securityUtils);
    }

    private void asUser(long id, String... roles) {
        when(securityUtils.getCurrentUser()).thenReturn(User.builder()
                .id(id)
                .username("user" + id)
                .email("user" + id + "@teasjul.local")
                .passwordHash("x")
                .firstName("User")
                .lastName(String.valueOf(id))
                .build());
        for (String role : roles) {
            when(securityUtils.hasRole(id, role)).thenReturn(true);
        }
    }

    @Test
    void ownerCanAskForOwnHouseSummary() {
        asUser(1L, SecurityUtils.ROLE_OWNER);
        when(houseService.getHouseSummary(12L))
                .thenReturn(new HouseSummaryResponse(10, 4, 5, 1, 3, 2));

        AssistantChatResponse response =
                service.chat(chat("What is the summary for house 12?"));

        assertThat(response.error()).isFalse();
        assertThat(response.reply()).contains("totalRooms");
        assertThat(response.toolCalls()).extracting(AiToolCall::name)
                .containsExactly("get_house_summary");
        verify(houseService).getHouseSummary(12L);
    }

    @Test
    void managerWithAssignedHouseCanRunTool() {
        asUser(2L, SecurityUtils.ROLE_MANAGER);
        when(houseService.getHouseSummary(5L))
                .thenReturn(new HouseSummaryResponse(4, 1, 2, 1, 1, 1));

        AssistantChatResponse response =
                service.chat(chat("What is the summary for house 5?"));

        assertThat(response.error()).isFalse();
        assertThat(response.reply()).contains("totalRooms");
        verify(houseAccessService).requireHouseAccess(5L);
    }

    @Test
    void managerWithoutAssignedHouseIsRefused() {
        asUser(2L, SecurityUtils.ROLE_MANAGER);
        doThrow(new AccessDeniedException("No access"))
                .when(houseAccessService).requireHouseAccess(77L);

        AssistantChatResponse response =
                service.chat(chat("What is the summary for house 77?"));

        assertThat(response.error()).isTrue();
        assertThat(response.reply()).isEqualTo(AssistantAuthorization.REFUSAL);
        verify(houseService, never()).getHouseSummary(any());
    }

    @Test
    void ownerCannotAccessAnotherOwnersHouse() {
        asUser(1L, SecurityUtils.ROLE_OWNER);
        doThrow(new AccessDeniedException("No access"))
                .when(houseAccessService).requireHouseAccess(99L);

        AssistantChatResponse response =
                service.chat(chat("What is the summary for house 99?"));

        assertThat(response.error()).isTrue();
        assertThat(response.reply()).isEqualTo(AssistantAuthorization.REFUSAL);
    }

    @Test
    void tenantCannotUseOwnerManagerTools() {
        asUser(3L, SecurityUtils.ROLE_TENANT);

        AssistantChatResponse response = service.chat(chat("How many vacant rooms exist?"));

        assertThat(response.error()).isTrue();
        assertThat(response.reply()).isEqualTo(AssistantAuthorization.REFUSAL);
        assertThat(response.toolCalls()).extracting(AiToolCall::name)
                .contains("get_vacant_rooms");
        verify(houseService, never()).getHouseRooms(any(), any(),
                any(RoomStatus.class), any(), any(Pageable.class));
    }

    @Test
    void tenantCanAskOwnBalance() {
        asUser(3L, SecurityUtils.ROLE_TENANT);
        when(tenantContextResolver.requireLinkedTenant(3L)).thenReturn(null);
        when(tenantDashboardService.getDashboard()).thenReturn(new TenantDashboardResponse(
                null, null, null,
                new TenantDashboardResponse.TenantDashboardFinancial(
                        new BigDecimal("120.00"), new BigDecimal("80.00"), 1, 1, 0),
                null));

        AssistantChatResponse response = service.chat(chat("How much do I owe?"));

        assertThat(response.error()).isFalse();
        assertThat(response.reply()).contains("currentDue");
        assertThat(response.toolCalls()).extracting(AiToolCall::name)
                .containsExactly("tenant_balance");
        verify(tenantContextResolver).requireLinkedTenant(3L);
    }

    @Test
    void multiRoleUserReceivesUnionOfTenantAndStaffTools() {
        asUser(5L, SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_TENANT);
        when(tenantContextResolver.requireLinkedTenant(5L)).thenReturn(null);
        when(tenantDashboardService.getMyInvoices(0, 20))
                .thenReturn(PageResponse.of(List.of(), 0, 20, 0, 0));
        when(houseService.getHouseSummary(12L))
                .thenReturn(new HouseSummaryResponse(10, 4, 5, 1, 3, 2));

        AssistantChatResponse invoices = service.chat(chat("Show me my invoices"));
        assertThat(invoices.error()).isFalse();
        assertThat(invoices.reply()).contains("total");
        assertThat(invoices.toolCalls()).extracting(AiToolCall::name)
                .containsExactly("tenant_invoices");

        AssistantChatResponse summary = service.chat(chat("What is the summary for house 12?"));
        assertThat(summary.error()).isFalse();
        assertThat(summary.reply()).contains("totalRooms");
        assertThat(summary.toolCalls()).extracting(AiToolCall::name)
                .containsExactly("get_house_summary");
    }

    @Test
    void unrecognizedMessageReturnsPlainMockReplyWithoutErrors() {
        asUser(1L, SecurityUtils.ROLE_OWNER);

        AssistantChatResponse response = service.chat(chat("hello there"));

        assertThat(response.error()).isFalse();
        assertThat(response.reply()).contains("Mock reply");
        assertThat(response.toolCalls()).isEmpty();
    }

    @Test
    void disabledAssistantThrowsForbiddenState() {
        asUser(1L, SecurityUtils.ROLE_OWNER);
        aiProperties.setEnabled(false);

        assertThatThrownBy(() -> service.chat(chat("hello")))
                .isInstanceOf(AssistantDisabledException.class);
    }

    @Test
    void historyIsForwardedToProvider() {
        asUser(1L, SecurityUtils.ROLE_OWNER);
        when(houseService.getHouseSummary(12L))
                .thenReturn(new HouseSummaryResponse(10, 4, 5, 1, 3, 2));

        AssistantChatRequest request = new AssistantChatRequest(
                "What is the summary for house 12?", "s-1",
                List.of(new com.example.spring_boot_project_api.dto.request.AssistantHistoryMessage(
                        "What houses do I have?", "user")));

        AssistantChatResponse response = service.chat(request);
        assertThat(response.toolCalls()).extracting(AiToolCall::name)
                .containsExactly("get_house_summary");
    }

    private static AssistantChatRequest chat(String message) {
        return new AssistantChatRequest(message, null, null);
    }

    // -------------------------------------------------------------------------
    // Streaming (chatStream) tests
    // -------------------------------------------------------------------------

    /** Captures listener events for assertion. */
    private static class CapturingListener
            implements com.example.spring_boot_project_api.service.assistant.AssistantStreamListener {
        final java.util.List<String> deltas = new java.util.ArrayList<>();
        final java.util.List<String> toolEvents = new java.util.ArrayList<>();
        AssistantChatResponse doneResponse;
        String errorMessage;

        @Override public void onDelta(String f) { deltas.add(f); }
        @Override public void onToolEvent(String l) { toolEvents.add(l); }
        @Override public void onDone(AssistantChatResponse r) { doneResponse = r; }
        @Override public void onError(String m) { errorMessage = m; }
    }

    @Test
    void chatStream_ownerReceivesDeltasThenDone() {
        asUser(1L, SecurityUtils.ROLE_OWNER);
        when(houseService.getHouseSummary(12L))
                .thenReturn(new HouseSummaryResponse(10, 4, 5, 1, 3, 2));

        // "hello there" doesn't match a tool — MockChatClient returns plain text
        CapturingListener listener = new CapturingListener();
        service.chatStream(chat("hello there"), listener);

        // MockChatClient.chatStream chunks the reply; at least one delta expected
        assertThat(listener.deltas).isNotEmpty();
        assertThat(listener.doneResponse).isNotNull();
        assertThat(listener.doneResponse.reply()).contains("Mock reply");
        assertThat(listener.doneResponse.error()).isFalse();
        assertThat(listener.errorMessage).isNull();
    }

    @Test
    void chatStream_tenantDeniedOwnerToolFiresOnDoneWithError() {
        asUser(3L, SecurityUtils.ROLE_TENANT);

        // "vacant rooms" matches get_vacant_rooms which TENANT is not allowed to use
        CapturingListener listener = new CapturingListener();
        service.chatStream(chat("How many vacant rooms exist?"), listener);

        assertThat(listener.doneResponse).isNotNull();
        assertThat(listener.doneResponse.error()).isTrue();
        assertThat(listener.doneResponse.reply()).isEqualTo(AssistantAuthorization.REFUSAL);
        // onError must NOT be called for a role-denial — that's a normal business outcome
        assertThat(listener.errorMessage).isNull();
    }

    @Test
    void chatStream_disabledAssistantFiresOnError() {
        asUser(1L, SecurityUtils.ROLE_OWNER);
        aiProperties.setEnabled(false);

        CapturingListener listener = new CapturingListener();
        service.chatStream(chat("hello"), listener);

        assertThat(listener.errorMessage).isNotNull();
        assertThat(listener.doneResponse).isNull();
    }
}