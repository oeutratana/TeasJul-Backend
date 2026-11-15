package com.example.spring_boot_project_api.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.TenantInvoiceDetailResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceDetailResponse.UtilityDetail;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceResponse;
import com.example.spring_boot_project_api.enums.InvoiceItemType;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.InvoiceItem;

@Component
public class TenantDashboardMapper {

    public TenantInvoiceResponse toListResponse(Invoice invoice) {
        BigDecimal rent = sumByType(invoice, InvoiceItemType.RENT);
        BigDecimal water = sumByType(invoice, InvoiceItemType.WATER);
        BigDecimal electricity = sumByType(invoice, InvoiceItemType.ELECTRICITY);
        BigDecimal other = invoice.getItems().stream()
                .filter(item -> item.getItemType() != InvoiceItemType.RENT
                        && item.getItemType() != InvoiceItemType.WATER
                        && item.getItemType() != InvoiceItemType.ELECTRICITY)
                .map(TenantDashboardMapper::amountOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TenantInvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNo(),
                monthLabel(invoice.getBillingYear(), invoice.getBillingMonth()),
                rent,
                water,
                electricity,
                other,
                invoice.getTotalAmount(),
                invoice.getStatus(),
                invoice.getDueDate());
    }

    public TenantInvoiceDetailResponse toDetailResponse(Invoice invoice, String houseName, String roomNumber,
            Map<InvoiceItemType, String> unitsByItemType) {
        BigDecimal rent = sumByType(invoice, InvoiceItemType.RENT);

        List<UtilityDetail> utilities = invoice.getItems().stream()
                .filter(item -> item.getItemType() == InvoiceItemType.WATER
                        || item.getItemType() == InvoiceItemType.ELECTRICITY)
                .map(item -> new UtilityDetail(
                        item.getItemType(),
                        item.getQuantity(),
                        unitsByItemType.get(item.getItemType()),
                        item.getAmount()))
                .toList();

        BigDecimal other = invoice.getItems().stream()
                .filter(item -> item.getItemType() == InvoiceItemType.INTERNET
                        || item.getItemType() == InvoiceItemType.PARKING
                        || item.getItemType() == InvoiceItemType.OTHER)
                .map(TenantDashboardMapper::amountOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TenantInvoiceDetailResponse(
                invoice.getId(),
                invoice.getInvoiceNo(),
                monthLabel(invoice.getBillingYear(), invoice.getBillingMonth()),
                houseName,
                roomNumber,
                rent,
                utilities,
                other,
                invoice.getTotalAmount(),
                invoice.getStatus(),
                invoice.getDueDate());
    }

    private static BigDecimal sumByType(Invoice invoice, InvoiceItemType type) {
        return invoice.getItems().stream()
                .filter(item -> item.getItemType() == type)
                .map(TenantDashboardMapper::amountOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal amountOf(InvoiceItem item) {
        return item.getAmount() == null ? BigDecimal.ZERO : item.getAmount();
    }

    private static String monthLabel(Integer year, Integer month) {
        return String.format("%04d-%02d", year, month);
    }
}