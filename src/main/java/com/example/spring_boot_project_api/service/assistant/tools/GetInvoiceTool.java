package com.example.spring_boot_project_api.service.assistant.tools;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.InvoiceResponse;
import com.example.spring_boot_project_api.service.InvoiceService;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.AssistantArguments;
import com.example.spring_boot_project_api.service.assistant.AssistantTool;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * OWNER/MANAGER — invoice detail for a single invoice. The existing
 * {@link InvoiceService#getInvoiceById} resolves the invoice only when the
 * authenticated user owns it or manages its house.
 */
@Component
@RequiredArgsConstructor
public class GetInvoiceTool implements AssistantTool {

    private final InvoiceService invoiceService;

    @Override
    public String name() {
        return "get_invoice";
    }

    @Override
    public String description() {
        return "Show the full detail of a single invoice by its ID.";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(SecurityUtils.ROLE_OWNER, SecurityUtils.ROLE_MANAGER);
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of("invoiceId", "ID of the invoice (required)");
    }

    @Override
    public AiToolResult execute(AssistantToolContext context, AssistantArguments args) {
        long invoiceId = args.requiredLong("invoiceId");

        InvoiceResponse invoice = invoiceService.getInvoiceById(invoiceId);
        return AiToolResult.success(name(), invoice);
    }
}