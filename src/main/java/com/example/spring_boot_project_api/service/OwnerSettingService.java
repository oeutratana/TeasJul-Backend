package com.example.spring_boot_project_api.service;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.dto.request.UpdateOwnerSettingsRequest;
import com.example.spring_boot_project_api.dto.request.UpdateThemeRequest;
import com.example.spring_boot_project_api.dto.response.OwnerContractInfoResponse;
import com.example.spring_boot_project_api.dto.response.OwnerSettingsResponse;
import com.example.spring_boot_project_api.dto.response.ThemeSettingsResponse;

public interface OwnerSettingService {

    OwnerSettingsResponse getSettings();

    OwnerSettingsResponse updateSettings(UpdateOwnerSettingsRequest request);

    ThemeSettingsResponse getTheme();

    ThemeSettingsResponse updateTheme(UpdateThemeRequest request);

    ThemeSettingsResponse resetTheme();

    BigDecimal getExchangeRate(Long ownerId);

    String getInvoicePrefix(Long ownerId);

    int getInvoiceDefaultDueDays(Long ownerId);

    String getTelegramBotToken(Long ownerId);

    String getTelegramChatId(Long ownerId);

    /**
     * The contract Party A identity configured by the given owner.
     *
     * <p>Read by rental-contract creation so that every contract stores a snapshot
     * of its own signer. Blank fields are returned blank rather than rejected, and
     * the caller decides what to do about them.</p>
     */
    OwnerContractInfoResponse getContractInfo(Long ownerId);
}
