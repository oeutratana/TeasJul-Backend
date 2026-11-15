package com.example.spring_boot_project_api.service.assistant.tools;

import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.InvoiceResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.InvoiceService;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantArguments;
import com.example.spring_boot_project_api.service.assistant.AssistantTool;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/** OWNER/MANAGER — filterable list of invoices across the user's accessible houses. */
@Component
@RequiredArgsConstructor
public class GetInvoicesTool implements AssistantTool {

    private static final int MAX_INVOICES = 20;

    private final InvoiceService invoiceService;
    private final HouseAccessService houseAccessService;

    @Override
    public String name() {
        return "get_invoices";
    }

    @Override
    public String description() {
        return "List invoices filtered by optional house, status, billing year/month or free-text search.";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER);
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of(
                "houseId", "Optional house scope",
                "status", "Optional invoice status: DRAFT, PENDING, PARTIAL, PAID, OVERDUE, CANCELLED",
                "year", "Optional billing year",
                "month", "Optional billing month 1-12",
                "search", "Optional free-text search",
                "size", "Optional page size (capped at " + MAX_INVOICES + ")");
    }

    @Override
    public AiToolResult execute(AssistantToolContext context, AssistantArguments args) {
        Long houseId = args.longValue("houseId").orElse(null);
        if (houseId != null) {
            houseAccessService.requireHouseAccess(houseId);
        }

        String search = args.string("search").orElse(null);
        InvoiceStatus status = args.string("status")
                .map(value -> InvoiceStatus.valueOf(value.toUpperCase()))
                .orElse(null);
        Integer year = args.intValue("year").orElse(null);
        Integer month = args.intValue("month").orElse(null);
        int size = Math.min(args.intValue("size").orElse(MAX_INVOICES), MAX_INVOICES);

        PageResponse<InvoiceResponse> page = invoiceService.getInvoices(houseId, search, status,
                year, month, null, null, null, PageRequest.of(0, size));

        return AiToolResult.success(name(), Map.of(
                "total", page.totalElements(),
                "invoices", page.content()));
    }
}