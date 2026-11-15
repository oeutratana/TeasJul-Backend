package com.example.spring_boot_project_api.service.assistant.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateHouseRequest;
import com.example.spring_boot_project_api.dto.response.HouseSummaryResponse;
import com.example.spring_boot_project_api.dto.response.InvoiceResponse;
import com.example.spring_boot_project_api.dto.response.ManagerDashboardResponse;
import com.example.spring_boot_project_api.dto.response.ManagerMonthlyReportResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.HouseService;
import com.example.spring_boot_project_api.service.InvoiceService;
import com.example.spring_boot_project_api.service.ManagerDashboardService;
import com.example.spring_boot_project_api.service.ManagerReportService;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantArguments;
import com.example.spring_boot_project_api.service.assistant.AssistantAuthorization;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssistantStaffToolSecurityTest {

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

    private static final AssistantToolContext OWNER =
            new AssistantToolContext(1L, Set.of(SecurityUtils.ROLE_OWNER));
    private static final AssistantToolContext MANAGER =
            new AssistantToolContext(2L, Set.of(SecurityUtils.ROLE_MANAGER));

    @Test
    void getMyHouses_delegatesToHouseServiceAndNeverWrites() {
        when(houseService.getMyHouses()).thenReturn(List.of());

        AiToolResult result = new GetMyHousesTool(houseService).execute(OWNER, args());

        assertThat(result.success()).isTrue();
        verify(houseService).getMyHouses();
        verify(houseService, never()).createHouse(any(CreateHouseRequest.class));
        verify(houseService, never()).updateHouse(any(), any(UpdateHouseRequest.class));
        verify(houseService, never()).deleteHouse(anyLong());
    }

    @Test
    void getHouseSummary_delegatesAndRechecksAccess() {
        when(houseService.getHouseSummary(12L))
                .thenReturn(new HouseSummaryResponse(10, 4, 5, 1, 3, 2));

        AiToolResult result = new GetHouseSummaryTool(houseService, houseAccessService)
                .execute(OWNER, args("houseId", "12"));

        assertThat(result.success()).isTrue();
        assertThat(((HouseSummaryResponse) result.data()).totalRooms()).isEqualTo(10);
        verify(houseAccessService).requireHouseAccess(12L);
        verify(houseService).getHouseSummary(12L);
        verify(houseService, never()).createHouse(any(CreateHouseRequest.class));
        verify(houseService, never()).updateHouse(any(), any(UpdateHouseRequest.class));
        verify(houseService, never()).deleteHouse(anyLong());
    }

    @Test
    void getHouseSummary_managerWithAssignedHouseSucceeds() {
        Long managerUserId = 2L;
        when(houseService.getHouseSummary(5L))
                .thenReturn(new HouseSummaryResponse(4, 1, 2, 1, 1, 1));

        AiToolResult result = new GetHouseSummaryTool(houseService, houseAccessService)
                .execute(new AssistantToolContext(managerUserId, Set.of(SecurityUtils.ROLE_MANAGER)),
                        args("houseId", "5"));

        assertThat(result.success()).isTrue();
        verify(houseAccessService).requireHouseAccess(5L);
    }

    @Test
    void getHouseSummary_rejectsHouseNotAccessible() {
        doThrow(new AccessDeniedException("No access"))
                .when(houseAccessService).requireHouseAccess(77L);

        assertThatThrownBy(() -> new GetHouseSummaryTool(houseService, houseAccessService)
                .execute(MANAGER, args("houseId", "77")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getHouseSummary_rejectsMissingHouseId() {
        assertThatThrownBy(() -> new GetHouseSummaryTool(houseService, houseAccessService)
                .execute(OWNER, args()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("houseId");
    }

    @Test
    void getHouseRooms_passesFiltersAndScopesToPageZero() {
        when(houseService.getHouseRooms(eq(12L), isNull(), eq(RoomStatus.AVAILABLE), isNull(),
                any(Pageable.class)))
                .thenReturn(roomPage(6));

        AiToolResult result = new GetHouseRoomsTool(houseService, houseAccessService)
                .execute(OWNER, args("houseId", "12", "status", "AVAILABLE"));

        assertThat(result.success()).isTrue();
        assertThat(((Map<?, ?>) result.data()).get("total")).isEqualTo(6L);
        verify(houseAccessService).requireHouseAccess(12L);
        verify(houseService).getHouseRooms(eq(12L), isNull(), eq(RoomStatus.AVAILABLE), isNull(),
                any(Pageable.class));
        verify(houseService, never()).createHouse(any(CreateHouseRequest.class));
    }

    @Test
    void getMonthlyReport_defaultsToCurrentPeriodWhenNotSupplied() {
        int year = java.time.YearMonth.now().getYear();
        int month = java.time.YearMonth.now().getMonthValue();
        when(managerReportService.getMonthlyReport(year, month, null))
                .thenReturn(monthlyReport(year, month));

        AiToolResult result = new GetMonthlyReportTool(managerReportService, houseAccessService)
                .execute(OWNER, args());

        assertThat(result.success()).isTrue();
        assertThat(((ManagerMonthlyReportResponse) result.data()).meta().year()).isEqualTo(year);
    }

    @Test
    void getMonthlyReport_passesExplicitPeriodAndOptionalHouseScope() {
        when(managerReportService.getMonthlyReport(2026, 9, 12L))
                .thenReturn(monthlyReport(2026, 9));

        AiToolResult result = new GetMonthlyReportTool(managerReportService, houseAccessService)
                .execute(OWNER, args("year", "2026", "month", "9", "houseId", "12"));

        assertThat(result.success()).isTrue();
        verify(houseAccessService).requireHouseAccess(12L);
        verify(managerReportService).getMonthlyReport(2026, 9, 12L);
    }

    @Test
    void getMonthlyReport_rejectsHouseOutsideScope() {
        doThrow(new AccessDeniedException("No access"))
                .when(houseAccessService).requireHouseAccess(88L);

        assertThatThrownBy(() -> new GetMonthlyReportTool(managerReportService, houseAccessService)
                .execute(MANAGER, args("houseId", "88")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getInvoices_passesFiltersAndNeverMutatesInvoices() {
        when(invoiceService.getInvoices(eq(12L), isNull(), eq(InvoiceStatus.PENDING),
                eq(2026), eq(9), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(PageResponse.of(List.of(), 0, 20, 0, 0));

        AiToolResult result = new GetInvoicesTool(invoiceService, houseAccessService)
                .execute(OWNER, args("houseId", "12", "status", "PENDING", "year", "2026",
                        "month", "9"));

        assertThat(result.success()).isTrue();
        verify(houseAccessService).requireHouseAccess(12L);
        assertInvoiceWritesNeverCalled();
    }

    @Test
    void getInvoices_rejectsInvalidStatus() {
        assertThatThrownBy(() -> new GetInvoicesTool(invoiceService, houseAccessService)
                .execute(OWNER, args("status", "NOT_A_STATUS")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NOT_A_STATUS");
    }

    @Test
    void getInvoice_returnsSingleInvoiceAndNeverMutates() {
        when(invoiceService.getInvoiceById(7L)).thenReturn(sampleInvoice());

        AiToolResult result = new GetInvoiceTool(invoiceService).execute(OWNER,
                args("invoiceId", "7"));

        assertThat(result.success()).isTrue();
        assertThat(((InvoiceResponse) result.data()).invoiceNo()).isEqualTo("INV-2026-0007");
        assertInvoiceWritesNeverCalled();
    }

    @Test
    void getDashboardSummary_delegatesWithoutHouseFilter() {
        when(managerDashboardService.getDashboard(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(sampleDashboard());

        AiToolResult result = new GetDashboardSummaryTool(managerDashboardService)
                .execute(OWNER, args());

        assertThat(result.success()).isTrue();
        assertThat(((ManagerDashboardResponse) result.data()).kpis().totalRooms()).isEqualTo(10);
        verify(managerDashboardService).getDashboard(isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void getVacantRooms_delegatesPerAccessibleHouseAndNeverWrites() {
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(10L, 11L));
        when(houseService.getHouseRooms(eq(10L), isNull(), eq(RoomStatus.AVAILABLE), isNull(),
                any(Pageable.class))).thenReturn(roomPage(2));
        when(houseService.getHouseRooms(eq(11L), isNull(), eq(RoomStatus.AVAILABLE), isNull(),
                any(Pageable.class))).thenReturn(roomPage(3));

        AiToolResult result = new GetVacantRoomsTool(houseService, houseAccessService)
                .execute(MANAGER, args());

        assertThat(result.success()).isTrue();
        Map<?, ?> data = (Map<?, ?>) result.data();
        assertThat(data.get("totalVacantRooms")).isEqualTo(5L);
        assertThat(((List<?>) data.get("houses"))).hasSize(2);
        verify(houseService, never()).createHouse(any(CreateHouseRequest.class));
    }

    @Test
    void getVacantRooms_canBeScopedToSingleHouse() {
        when(houseService.getHouseRooms(eq(12L), isNull(), eq(RoomStatus.AVAILABLE), isNull(),
                any(Pageable.class))).thenReturn(roomPage(1));

        AiToolResult result = new GetVacantRoomsTool(houseService, houseAccessService)
                .execute(OWNER, args("houseId", "12"));

        assertThat(result.success()).isTrue();
        assertThat(((Map<?, ?>) result.data()).get("totalVacantRooms")).isEqualTo(1L);
        verify(houseAccessService).requireHouseAccess(12L);
    }

    @Test
    void getOverdueInvoices_sumsOutstandingAndNeverMutates() {
        when(invoiceService.getInvoices(isNull(), isNull(), eq(InvoiceStatus.OVERDUE),
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(PageResponse.of(List.of(sampleInvoice()), 0, 100, 1, 1));

        AiToolResult result = new GetOverdueInvoicesTool(invoiceService, houseAccessService)
                .execute(OWNER, args());

        assertThat(result.success()).isTrue();
        Map<?, ?> data = (Map<?, ?>) result.data();
        assertThat(data.get("totalOverdue")).isEqualTo(1L);
        assertThat(data.get("totalOutstanding")).isEqualTo(new BigDecimal("90.00"));
        assertInvoiceWritesNeverCalled();
    }

    private void assertInvoiceWritesNeverCalled() {
        verify(invoiceService, never()).generateMonthlyInvoice(any());
        verify(invoiceService, never()).generateBulkInvoices(any());
        verify(invoiceService, never()).addItem(anyLong(), any());
        verify(invoiceService, never()).updateStatus(anyLong(), any(InvoiceStatus.class));
        verify(invoiceService, never()).cancelInvoice(anyLong());
        verify(invoiceService, never()).markPaid(anyLong());
        verify(invoiceService, never()).recordPayment(anyLong(), any());
    }

    private static AssistantArguments args(String... pairs) {
        java.util.LinkedHashMap<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return new AssistantArguments(map);
    }

    private static PageResponse<RoomResponse> roomPage(long total) {
        return PageResponse.of(List.of(), 0, 100, total, 1);
    }

    private static InvoiceResponse sampleInvoice() {
        return new InvoiceResponse(
                7L, 1L, "INV-2026-0007", 5L, "C-0005", 3L, "Dara", 6L, "A-101", 12L, "House Sun",
                2026, 9, LocalDate.of(2026, 9, 5),
                new BigDecimal("100.00"), new BigDecimal("10.00"), BigDecimal.ZERO,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"),
                InvoiceStatus.OVERDUE, null, null, null, null, List.of());
    }

    private static ManagerDashboardResponse sampleDashboard() {
        return new ManagerDashboardResponse(
                new ManagerDashboardResponse.Kpis(10, 4, 5, 1, new BigDecimal("1250.00"), 50.0, 3, 2),
                List.of(new ManagerDashboardResponse.RevenuePoint("2026-08", new BigDecimal("1250.00"))),
                List.of(new ManagerDashboardResponse.RoomOccupancyPoint("House #8", 2, 2, 1)),
                List.of(new ManagerDashboardResponse.StatusCount("PAID", 6)),
                List.of(new ManagerDashboardResponse.StatusCount("PENDING", 1)));
    }

    private static ManagerMonthlyReportResponse monthlyReport(int year, int month) {
        LocalDateTime now = LocalDateTime.now();
        return new ManagerMonthlyReportResponse(
                new ManagerMonthlyReportResponse.ReportMeta(year, month, now, now, null, null, now),
                new ManagerMonthlyReportResponse.RoomStats(0, 0, 0, 0, 0, 0.0),
                new ManagerMonthlyReportResponse.FinancialSummary(BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new ManagerMonthlyReportResponse.ContractSummary(0, 0, 0, 0, 0),
                new ManagerMonthlyReportResponse.PaymentSummary(0, 0, 0, BigDecimal.ZERO),
                new ManagerMonthlyReportResponse.UtilitySummary(BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, 0, 0),
                new ManagerMonthlyReportResponse.MaintenanceSummary(0, 0, 0, 0, 0, BigDecimal.ZERO),
                List.of(),
                List.of(new ManagerMonthlyReportResponse.StatusCount("PENDING", 9)),
                List.of(),
                List.of());
    }
}
