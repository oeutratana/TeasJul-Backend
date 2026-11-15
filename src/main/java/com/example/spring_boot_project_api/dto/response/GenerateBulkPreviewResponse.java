package com.example.spring_boot_project_api.dto.response;

/**
 * Counts shown before a bulk generation run: how many ACTIVE contracts exist in
 * the selected house scope, how many of them already have an invoice for the
 * period, and how many would therefore be generated.
 */
public record GenerateBulkPreviewResponse(
        long activeContracts,
        long alreadyInvoiced,
        long toGenerate,
        int month,
        int year
) {
}
