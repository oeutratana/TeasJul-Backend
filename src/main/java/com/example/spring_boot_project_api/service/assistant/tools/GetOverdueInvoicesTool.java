package com.example.spring_boot_project_api.service.assistant.tools;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

/**
 * OWNER/MANAGER — overdue invoices with the total outstanding amount
 * (sum of {@code totalAmount - discount}) across inaccessible-safe scoping.
 */
@Component
@RequiredArgsConstructor
public class GetOverdueInvoicesTool implements AssistantTool {

    private static final int MAX_INVOICES = 100;

    private final InvoiceService invoiceService;
    private final HouseAccessService houseAccessService;

    @Override
    public String name() {
        return "get_overdue_invoices";
    }

    @Override
    public String description() {
        return "List overdue invoices and the total outstanding balance they represent.";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER);
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of(
                "houseId", "Optional single-house scope",
                "year", "Optional billing year",
                "month", "Optional billing month 1-12");
    }

    @Override
    public AiToolResult execute(AssistantToolContext context, AssistantArguments args) {
        Long houseId = args.longValue("houseId").orElse(null);
        if (houseId != null) {
            houseAccessService.requireHouseAccess(houseId);
        }
        Integer year = args.intValue("year").orElse(null);
        Integer month = args.intValue("month").orElse(null);

        PageResponse<InvoiceResponse> page = invoiceService.getInvoices(houseId, null,
                InvoiceStatus.OVERDUE, year, month, null, null, null, PageRequest.of(0, MAX_INVOICES));

        BigDecimal totalOutstanding = BigDecimal.ZERO;
        List<Map<String, Object>> invoices = new ArrayList<>();
        for (InvoiceResponse invoice : page.content()) {
            BigDecimal net = invoice.totalAmount().subtract(invoice.discount() == null
                    ? BigDecimal.ZERO : invoice.discount());
            totalOutstanding = totalOutstanding.add(net);

            Map<String, Object> slim = new LinkedHashMap<>();
            slim.put("invoiceNo", invoice.invoiceNo());
            slim.put("tenantName", invoice.tenantName());
            slim.put("roomNumber", invoice.roomNumber());
            slim.put("houseName", invoice.houseName());
            slim.put("billingMonth", invoice.billingMonth());
            slim.put("totalAmount", invoice.totalAmount());
            slim.put("dueDate", invoice.dueDate());
            invoices.add(slim);
        }

        return AiToolResult.success(name(), Map.of(
                "totalOverdue", page.totalElements(),
                "totalOutstanding", totalOutstanding,
                "invoices", invoices));
    }
}