package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.spring_boot_project_api.dto.response.TenantInvoiceDetailResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceResponse;
import com.example.spring_boot_project_api.enums.InvoiceItemType;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.mapper.TenantDashboardMapper;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.InvoiceItem;

class TenantDashboardMapperTest {

    private final TenantDashboardMapper mapper = new TenantDashboardMapper();

    private Invoice invoiceWithItems() {
        Invoice invoice = Invoice.builder()
                .id(10L)
                .invoiceNo("INV-2026-0010")
                .contractId(100L)
                .billingYear(2026)
                .billingMonth(8)
                .dueDate(LocalDate.of(2026, 8, 5))
                .subtotal(new BigDecimal("90.00"))
                .discount(BigDecimal.ZERO)
                .lateFee(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("90.00"))
                .status(InvoiceStatus.PENDING)
                .build();
        invoice.getItems().add(item(InvoiceItemType.RENT, "Rent", new BigDecimal("60.00")));
        invoice.getItems().add(item(InvoiceItemType.WATER, "Water", new BigDecimal("5.00")));
        invoice.getItems().add(item(InvoiceItemType.ELECTRICITY, "Electricity", new BigDecimal("20.00")));
        invoice.getItems().add(item(InvoiceItemType.INTERNET, "Internet", new BigDecimal("5.00")));
        return invoice;
    }

    private static InvoiceItem item(InvoiceItemType type, String description, BigDecimal amount) {
        return InvoiceItem.builder()
                .itemType(type)
                .description(description)
                .quantity(BigDecimal.ONE)
                .unitPrice(amount)
                .amount(amount)
                .build();
    }

    @Test
    void toListResponseSplitsAmountsByType() {
        TenantInvoiceResponse response = mapper.toListResponse(invoiceWithItems());

        assertThat(response.invoiceNo()).isEqualTo("INV-2026-0010");
        assertThat(response.billingMonth()).isEqualTo("2026-08");
        assertThat(response.rent()).isEqualByComparingTo("60.00");
        assertThat(response.water()).isEqualByComparingTo("5.00");
        assertThat(response.electricity()).isEqualByComparingTo("20.00");
        assertThat(response.otherCharges()).isEqualByComparingTo("5.00");
        assertThat(response.total()).isEqualByComparingTo("90.00");
    }

    @Test
    void toDetailResponseBuildsUtilitiesBreakdown() {
        Map<InvoiceItemType, String> units = new EnumMap<>(InvoiceItemType.class);
        units.put(InvoiceItemType.WATER, "m3");
        units.put(InvoiceItemType.ELECTRICITY, "kWh");

        TenantInvoiceDetailResponse response = mapper.toDetailResponse(
                invoiceWithItems(), "Golden House", "A101", units);

        assertThat(response.houseName()).isEqualTo("Golden House");
        assertThat(response.roomNumber()).isEqualTo("A101");
        assertThat(response.rent()).isEqualByComparingTo("60.00");
        assertThat(response.utilities()).hasSize(2);
        assertThat(response.utilities().get(0).type()).isEqualTo(InvoiceItemType.WATER);
        assertThat(response.utilities().get(0).usage()).isEqualByComparingTo("1");
        assertThat(response.utilities().get(0).unit()).isEqualTo("m3");
        assertThat(response.utilities().get(1).type()).isEqualTo(InvoiceItemType.ELECTRICITY);
        assertThat(response.utilities().get(1).unit()).isEqualTo("kWh");
        assertThat(response.otherCharges()).isEqualByComparingTo("5.00");
        assertThat(response.total()).isEqualByComparingTo("90.00");
    }
}