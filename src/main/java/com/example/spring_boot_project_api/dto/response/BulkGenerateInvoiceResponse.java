package com.example.spring_boot_project_api.dto.response;

import java.util.List;

public record BulkGenerateInvoiceResponse(
        int createdCount,
        int skippedCount,
        int errorCount,
        List<InvoiceResponse> createdInvoices,
        List<String> skippedReasons,
        List<String> errors
) {
}
