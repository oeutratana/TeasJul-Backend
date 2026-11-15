package com.example.spring_boot_project_api.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.InvoiceItemResponse;
import com.example.spring_boot_project_api.dto.response.InvoiceResponse;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.InvoiceItem;
import com.example.spring_boot_project_api.util.InvoiceBalanceCalculator;

@Component
public class InvoiceMapper {

    public InvoiceItemResponse toItemResponse(InvoiceItem item) {
        return new InvoiceItemResponse(
                item.getId(),
                item.getInvoice().getId(),
                item.getItemType(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getAmount());
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        return toResponse(invoice, null, BigDecimal.ZERO);
    }

    public InvoiceResponse toResponse(Invoice invoice, Object[] details) {
        return toResponse(invoice, details, BigDecimal.ZERO);
    }

    public InvoiceResponse toResponse(Invoice invoice, Object[] details, BigDecimal paidAmount) {
        Long contractId = invoice.getContractId();
        String contractNo = null;
        Long tenantId = null;
        String tenantName = null;
        Long roomId = null;
        String roomNumber = null;
        Long houseId = null;
        String houseName = null;

        if (details != null) {
            // details = [contractNo, tenantId, tenantName, roomId, roomNumber, houseId, houseName]
            if (details.length > 0 && details[0] != null) contractNo = details[0].toString();
            if (details.length > 1 && details[1] != null) tenantId = ((Number) details[1]).longValue();
            if (details.length > 2 && details[2] != null) tenantName = details[2].toString();
            if (details.length > 3 && details[3] != null) roomId = ((Number) details[3]).longValue();
            if (details.length > 4 && details[4] != null) roomNumber = details[4].toString();
            if (details.length > 5 && details[5] != null) houseId = ((Number) details[5]).longValue();
            if (details.length > 6 && details[6] != null) houseName = details[6].toString();
        }

        List<InvoiceItemResponse> items = invoice.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal totalAmount = InvoiceBalanceCalculator.amount(invoice.getTotalAmount());
        BigDecimal paid = InvoiceBalanceCalculator.amount(paidAmount);

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getOwner().getId(),
                invoice.getInvoiceNo(),
                contractId,
                contractNo,
                tenantId,
                tenantName,
                roomId,
                roomNumber,
                houseId,
                houseName,
                invoice.getBillingYear(),
                invoice.getBillingMonth(),
                invoice.getDueDate(),
                invoice.getSubtotal(),
                invoice.getDiscount(),
                invoice.getLateFee(),
                totalAmount,
                paid,
                InvoiceBalanceCalculator.remainingBalance(totalAmount, paid),
                invoice.getStatus(),
                invoice.getExchangeRate(),
                invoice.getExchangeRateCurrency(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt(),
                items);
    }
}
