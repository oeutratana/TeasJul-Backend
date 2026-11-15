package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.UpdateOwnerContractInfoRequest;
import com.example.spring_boot_project_api.dto.request.UpdateOwnerSettingsRequest;
import com.example.spring_boot_project_api.dto.request.UpdateThemeRequest;
import com.example.spring_boot_project_api.dto.response.OwnerContractInfoResponse;
import com.example.spring_boot_project_api.dto.response.OwnerSettingsResponse;
import com.example.spring_boot_project_api.dto.response.ThemeSettingsResponse;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.model.Gender;
import com.example.spring_boot_project_api.model.OwnerSetting;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.OwnerSettingRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.OwnerSettingService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OwnerSettingServiceImpl implements OwnerSettingService {

    private static final Logger log = LoggerFactory.getLogger(OwnerSettingServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String THEME_DEFAULT_MODE = "LIGHT";
    private static final String THEME_DEFAULT_PRIMARY = "#2563EB";
    private static final String THEME_DEFAULT_SECONDARY = "#0F172A";
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    /** Key of the owner's contract Party A identity block inside the settings document. */
    private static final String CONTRACT_PARTY = "contractParty";

    private final OwnerSettingRepository ownerSettingRepository;
    private final SecurityUtils securityUtils;
    private final TenantRepository tenantRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final UserRoleRepository userRoleRepository;
    private final SecurityAuditLogger securityAuditLogger;

    @Override
    @Transactional(readOnly = true)
    public OwnerSettingsResponse getSettings() {
        securityUtils.requirePermission(PermissionConstants.SETTING_VIEW);
        User owner = getCurrentOwner();
        OwnerSetting setting = ownerSettingRepository.findByOwnerId(owner.getId())
                .orElseGet(() -> createDefaultSettings(owner));
        return toResponse(setting);
    }

    @Override
    @Transactional
    public OwnerSettingsResponse updateSettings(UpdateOwnerSettingsRequest request) {
        securityUtils.requirePermission(PermissionConstants.SETTING_UPDATE);
        User owner = getCurrentOwner();
        OwnerSetting setting = ownerSettingRepository.findByOwnerId(owner.getId())
                .orElseGet(() -> createDefaultSettings(owner));

        try {
            ObjectNode root = OBJECT_MAPPER.createObjectNode();

            // General
            root.put("baseCurrency", request.baseCurrency());
            root.put("displayCurrency", request.displayCurrency());
            root.put("exchangeRate", request.exchangeRate().doubleValue());
            root.put("dateFormat", request.dateFormat());
            root.put("timezone", request.timezone());

            // Branding
            putIfNotNull(root, "businessName", request.businessName());
            putIfNotNull(root, "logoUrl", request.logoUrl());
            putIfNotNull(root, "phone", request.phone());
            putIfNotNull(root, "email", request.email());
            putIfNotNull(root, "address", request.address());
            putIfNotNull(root, "primaryColor", request.primaryColor());
            putIfNotNull(root, "secondaryColor", request.secondaryColor());

            // Document Language
            root.put("documentLanguage", request.documentLanguage());

            // Invoice
            putIfNotNull(root, "invoicePrefix", request.invoicePrefix());
            if (request.invoiceDefaultDueDays() != null) {
                root.put("invoiceDefaultDueDays", request.invoiceDefaultDueDays());
            }
            putIfNotNull(root, "invoiceFooter", request.invoiceFooter());
            root.put("invoiceShowUsd", Boolean.TRUE.equals(request.invoiceShowUsd()));
            root.put("invoiceShowKhr", Boolean.TRUE.equals(request.invoiceShowKhr()));
            root.put("invoiceShowLogo", Boolean.TRUE.equals(request.invoiceShowLogo()));
            root.put("invoiceShowBusinessInfo", Boolean.TRUE.equals(request.invoiceShowBusinessInfo()));

            // Contract
            putIfNotNull(root, "contractTitle", request.contractTitle());
            putIfNotNull(root, "contractPaymentTerms", request.contractPaymentTerms());
            putIfNotNull(root, "contractDepositTerms", request.contractDepositTerms());
            putIfNotNull(root, "contractTerminationTerms", request.contractTerminationTerms());
            putIfNotNull(root, "contractAdditionalTerms", request.contractAdditionalTerms());
            putIfNotNull(root, "contractLanguage", request.contractLanguage());

            // Contract Party A (the owner's own identity). Preserved as-is when the
            // caller omits the block, so a partial save never wipes a signer's identity.
            JsonNode existing = readOrNull(setting.getSettings());
            UpdateOwnerContractInfoRequest contractParty = request.contractParty();
            if (contractParty != null) {
                root.set(CONTRACT_PARTY, contractPartyNode(contractParty));
            } else if (existing != null && existing.has(CONTRACT_PARTY)) {
                root.set(CONTRACT_PARTY, existing.get(CONTRACT_PARTY));
            }

            // Receipt
            putIfNotNull(root, "receiptPrefix", request.receiptPrefix());
            putIfNotNull(root, "receiptLanguage", request.receiptLanguage());
            putIfNotNull(root, "receiptFooter", request.receiptFooter());
            root.put("receiptShowPaymentMethod", Boolean.TRUE.equals(request.receiptShowPaymentMethod()));
            root.put("receiptShowTransactionId", Boolean.TRUE.equals(request.receiptShowTransactionId()));
            root.put("receiptShowUsd", Boolean.TRUE.equals(request.receiptShowUsd()));
            root.put("receiptShowKhr", Boolean.TRUE.equals(request.receiptShowKhr()));

            // Telegram
            putIfNotNull(root, "telegramBotToken", request.telegramBotToken());
            putIfNotNull(root, "telegramChatId", request.telegramChatId());

            // Theme (managed by the dedicated theme endpoints) is preserved as-is
            if (existing != null && existing.has("theme")) {
                root.set("theme", existing.get("theme"));
            }

            setting.setSettings(OBJECT_MAPPER.writeValueAsString(root));
        } catch (Exception e) {
            log.error("Failed to serialize settings", e);
            throw new RuntimeException("Failed to save settings", e);
        }

        OwnerSetting saved = ownerSettingRepository.save(setting);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ThemeSettingsResponse getTheme() {
        Long ownerId = resolveOrganizationOwnerId();
        if (ownerId == null) {
            return themeDefaults(null);
        }
        return ownerSettingRepository.findByOwnerId(ownerId)
                .map(this::themeFromSetting)
                .orElseGet(() -> themeDefaults(ownerId));
    }

    @Override
    @Transactional
    public ThemeSettingsResponse updateTheme(UpdateThemeRequest request) {
        securityUtils.requirePermission(PermissionConstants.SETTING_UPDATE);
        User actor = securityUtils.getCurrentUser();
        User themeOwner = resolveThemeOwner();
        OwnerSetting setting = ownerSettingRepository.findByOwnerId(themeOwner.getId())
                .orElseGet(() -> createDefaultSettings(themeOwner));
        try {
            ObjectNode root = (ObjectNode) OBJECT_MAPPER.readTree(setting.getSettings());
            ObjectNode theme = root.putObject("theme");
            theme.put("themeMode", normalizeMode(request.themeMode()));
            theme.put("primaryColor", normalizeHex(request.primaryColor()));
            theme.put("secondaryColor", normalizeHex(request.secondaryColor()));
            setting.setSettings(OBJECT_MAPPER.writeValueAsString(root));

            OwnerSetting saved = ownerSettingRepository.save(setting);
            ThemeSettingsResponse themeSettings = themeFromSetting(saved);
            securityAuditLogger.log(actor.getId(), "THEME_UPDATED", themeDetail(themeSettings));
            return themeSettings;
        } catch (Exception e) {
            log.error("Failed to save theme for owner: {}", themeOwner.getId(), e);
            throw new RuntimeException("Failed to save theme", e);
        }
    }

    @Override
    @Transactional
    public ThemeSettingsResponse resetTheme() {
        securityUtils.requirePermission(PermissionConstants.SETTING_UPDATE);
        User actor = securityUtils.getCurrentUser();
        User themeOwner = resolveThemeOwner();
        OwnerSetting setting = ownerSettingRepository.findByOwnerId(themeOwner.getId())
                .orElseGet(() -> createDefaultSettings(themeOwner));
        try {
            ObjectNode root = (ObjectNode) OBJECT_MAPPER.readTree(setting.getSettings());
            ObjectNode theme = root.putObject("theme");
            theme.put("themeMode", THEME_DEFAULT_MODE);
            theme.put("primaryColor", THEME_DEFAULT_PRIMARY);
            theme.put("secondaryColor", THEME_DEFAULT_SECONDARY);
            setting.setSettings(OBJECT_MAPPER.writeValueAsString(root));

            OwnerSetting saved = ownerSettingRepository.save(setting);
            ThemeSettingsResponse themeSettings = themeFromSetting(saved);
            securityAuditLogger.log(actor.getId(), "THEME_RESET", themeDetail(themeSettings));
            return themeSettings;
        } catch (Exception e) {
            log.error("Failed to reset theme for owner: {}", themeOwner.getId(), e);
            throw new RuntimeException("Failed to reset theme", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getExchangeRate(Long ownerId) {
        return ownerSettingRepository.findByOwnerId(ownerId)
                .map(s -> {
                    try {
                        JsonNode root = OBJECT_MAPPER.readTree(s.getSettings());
                        double rate = root.has("exchangeRate") ? root.get("exchangeRate").asDouble() : 4100.0;
                        return BigDecimal.valueOf(rate);
                    } catch (Exception e) {
                        return BigDecimal.valueOf(4100.0);
                    }
                })
                .orElse(BigDecimal.valueOf(4100.0));
    }

    @Override
    @Transactional(readOnly = true)
    public String getInvoicePrefix(Long ownerId) {
        return ownerSettingRepository.findByOwnerId(ownerId)
                .map(s -> {
                    try {
                        JsonNode root = OBJECT_MAPPER.readTree(s.getSettings());
                        return root.has("invoicePrefix") ? root.get("invoicePrefix").asText("INV") : "INV";
                    } catch (Exception e) {
                        return "INV";
                    }
                })
                .orElse("INV");
    }

    @Override
    @Transactional(readOnly = true)
    public int getInvoiceDefaultDueDays(Long ownerId) {
        return ownerSettingRepository.findByOwnerId(ownerId)
                .map(s -> {
                    try {
                        JsonNode root = OBJECT_MAPPER.readTree(s.getSettings());
                        return root.has("invoiceDefaultDueDays") ? root.get("invoiceDefaultDueDays").asInt(5) : 5;
                    } catch (Exception e) {
                        return 5;
                    }
                })
                .orElse(5);
    }

    @Override
    @Transactional(readOnly = true)
    public String getTelegramBotToken(Long ownerId) {
        return ownerSettingRepository.findByOwnerId(ownerId)
                .map(s -> {
                    try {
                        JsonNode root = OBJECT_MAPPER.readTree(s.getSettings());
                        return root.has("telegramBotToken") ? root.get("telegramBotToken").asText("") : "";
                    } catch (Exception e) {
                        return "";
                    }
                })
                .orElse("");
    }

    @Override
    @Transactional(readOnly = true)
    public String getTelegramChatId(Long ownerId) {
        return ownerSettingRepository.findByOwnerId(ownerId)
                .map(s -> {
                    try {
                        JsonNode root = OBJECT_MAPPER.readTree(s.getSettings());
                        return root.has("telegramChatId") ? root.get("telegramChatId").asText("") : "";
                    } catch (Exception e) {
                        return "";
                    }
                })
                .orElse("");
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerContractInfoResponse getContractInfo(Long ownerId) {
        if (ownerId == null) {
            return emptyContractInfo();
        }
        return ownerSettingRepository.findByOwnerId(ownerId)
                .map(s -> {
                    try {
                        return contractInfoFrom(readOrNull(s.getSettings()));
                    } catch (Exception e) {
                        log.error("Failed to read contract info for owner: {}", ownerId, e);
                        return emptyContractInfo();
                    }
                })
                .orElseGet(OwnerSettingServiceImpl::emptyContractInfo);
    }

    private User getCurrentOwner() {
        User user = securityUtils.getCurrentUser();
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            throw new AccessDeniedException("Only owners can manage settings");
        }
        return user;
    }

    /**
     * Resolves the organization (owner) the current user belongs to, so that all
     * roles in the same organization see the owner's saved theme. The owner is
     * always derived from the authenticated context - never from the client.
     *
     * <p>A SUPER_ADMIN belongs to no organization, so it resolves the system
     * owner: the theme stays a single shared value that the owner and the super
     * admin always read and write on the same settings row.</p>
     */
    private Long resolveOrganizationOwnerId() {
        User user = securityUtils.getCurrentUser();
        if (securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            return user.getId();
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            User systemOwner = findSystemOwner();
            return systemOwner == null ? null : systemOwner.getId();
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_TENANT)) {
            return tenantRepository.findByUserId(user.getId())
                    .map(tenant -> tenant.getOwner().getId())
                    .orElse(null);
        }
        return userHouseAccessRepository.findOwnerIdsByUserId(user.getId())
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves the owner whose settings row backs the shared system theme and
     * asserts the caller may edit it. OWNER edits its own row; SUPER_ADMIN
     * edits the system owner's row so both roles share one persisted value.
     * MANAGER and TENANT are rejected.
     */
    private User resolveThemeOwner() {
        User user = securityUtils.getCurrentUser();
        if (securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            return user;
        }
        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            User systemOwner = findSystemOwner();
            if (systemOwner == null) {
                throw new IllegalStateException("No owner is configured to hold the system theme");
            }
            return systemOwner;
        }
        throw new AccessDeniedException("Only owners and super admins can manage the theme");
    }

    /**
     * The system-wide owner backing the shared theme: the lowest-id user holding
     * the OWNER role, so the choice is stable across requests and processes.
     */
    private User findSystemOwner() {
        return userRoleRepository.findUsersByRoleCode(SecurityUtils.ROLE_OWNER)
                .stream()
                .min(Comparator.comparing(User::getId))
                .orElse(null);
    }

    private ThemeSettingsResponse themeDefaults(Long ownerId) {
        return new ThemeSettingsResponse(ownerId, THEME_DEFAULT_MODE, THEME_DEFAULT_PRIMARY, THEME_DEFAULT_SECONDARY);
    }

    private ThemeSettingsResponse themeFromSetting(OwnerSetting setting) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(setting.getSettings());
            JsonNode theme = root.has("theme") ? root.get("theme") : null;

            String mode = theme != null && theme.has("themeMode")
                    ? theme.get("themeMode").asText(THEME_DEFAULT_MODE)
                    : THEME_DEFAULT_MODE;

            String primary;
            if (theme != null && theme.has("primaryColor")) {
                primary = normalizeHex(theme.get("primaryColor").asText(THEME_DEFAULT_PRIMARY));
            } else {
                String brandingPrimary = normalizeHex(getTextOrDefault(root, "primaryColor", ""));
                primary = HEX_COLOR.matcher(brandingPrimary).matches() ? brandingPrimary : THEME_DEFAULT_PRIMARY;
            }

            String secondary;
            if (theme != null && theme.has("secondaryColor")) {
                secondary = normalizeHex(theme.get("secondaryColor").asText(THEME_DEFAULT_SECONDARY));
            } else {
                String brandingSecondary = normalizeHex(getTextOrDefault(root, "secondaryColor", ""));
                secondary = HEX_COLOR.matcher(brandingSecondary).matches() ? brandingSecondary : THEME_DEFAULT_SECONDARY;
            }

            return new ThemeSettingsResponse(
                    setting.getOwner().getId(), normalizeMode(mode), primary, secondary);
        } catch (Exception e) {
            log.error("Failed to read theme for owner: {}", setting.getOwner().getId(), e);
            return themeDefaults(setting.getOwner().getId());
        }
    }

    private String normalizeMode(String mode) {
        return mode == null || mode.isBlank() ? THEME_DEFAULT_MODE : mode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeHex(String color) {
        if (color == null || !HEX_COLOR.matcher(color).matches()) {
            return color;
        }
        return color.toUpperCase(Locale.ROOT);
    }

    private String themeDetail(ThemeSettingsResponse theme) {
        return "mode=%s primary=%s secondary=%s".formatted(
                theme.themeMode(), theme.primaryColor(), theme.secondaryColor());
    }

    private JsonNode readOrNull(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private OwnerSetting createDefaultSettings(User owner) {
        try {
            ObjectNode root = OBJECT_MAPPER.createObjectNode();
            root.put("baseCurrency", "USD");
            root.put("displayCurrency", "USD");
            root.put("exchangeRate", 4100.0);
            root.put("dateFormat", "DD/MM/YYYY");
            root.put("timezone", "Asia/Phnom_Penh");
            root.put("businessName", "");
            root.put("logoUrl", "");
            root.put("phone", "");
            root.put("email", "");
            root.put("address", "");
            root.put("primaryColor", "#3b82f6");
            root.put("secondaryColor", "#10b981");
            root.put("documentLanguage", "KM");
            root.put("invoicePrefix", "INV");
            root.put("invoiceDefaultDueDays", 10);
            root.put("invoiceFooter", "");
            root.put("invoiceShowUsd", true);
            root.put("invoiceShowKhr", true);
            root.put("invoiceShowLogo", true);
            root.put("invoiceShowBusinessInfo", true);
            root.put("contractTitle", "កិច្ចសន្យាជួលផ្ទះ");
            root.put("contractPaymentTerms", "");
            root.put("contractDepositTerms", "");
            root.put("contractTerminationTerms", "");
            root.put("contractAdditionalTerms", "");
            root.put("contractLanguage", "KM");
            root.set(CONTRACT_PARTY, defaultContractParty(owner));
            root.put("receiptPrefix", "RCP");
            root.put("receiptLanguage", "KM");
            root.put("receiptFooter", "");
            root.put("receiptShowPaymentMethod", true);
            root.put("receiptShowTransactionId", true);
            root.put("receiptShowUsd", true);
            root.put("receiptShowKhr", true);
            root.put("telegramBotToken", "");
            root.put("telegramChatId", "");

            ObjectNode theme = root.putObject("theme");
            theme.put("themeMode", THEME_DEFAULT_MODE);
            theme.put("primaryColor", THEME_DEFAULT_PRIMARY);
            theme.put("secondaryColor", THEME_DEFAULT_SECONDARY);

            OwnerSetting setting = OwnerSetting.builder()
                    .owner(owner)
                    .settings(OBJECT_MAPPER.writeValueAsString(root))
                    .build();
            return ownerSettingRepository.save(setting);
        } catch (Exception e) {
            log.error("Failed to create default settings for owner: {}", owner.getId(), e);
            throw new RuntimeException("Failed to initialize settings", e);
        }
    }

    private void putIfNotNull(ObjectNode node, String key, String value) {
        if (value != null) {
            node.put(key, value);
        }
    }

    /**
     * Serializes the owner's contract Party A block. Text is trimmed and blank
     * values are written as empty strings so the stored document always has the
     * same shape; optional identity fields are never required.
     */
    private ObjectNode contractPartyNode(UpdateOwnerContractInfoRequest party) {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        node.put("fullName", trimToEmpty(party.fullName()));
        if (party.gender() != null) {
            node.put("gender", party.gender().name());
        } else {
            node.putNull("gender");
        }
        if (party.dateOfBirth() != null) {
            node.put("dateOfBirth", party.dateOfBirth().toString());
        } else {
            node.putNull("dateOfBirth");
        }
        node.put("nationality", trimToEmpty(party.nationality()));
        node.put("nationalId", trimToEmpty(party.nationalId()));
        node.put("currentAddress", trimToEmpty(party.currentAddress()));
        node.put("phone", trimToEmpty(party.phone()));
        node.put("occupation", trimToEmpty(party.occupation()));
        return node;
    }

    /**
     * Seeds a brand-new settings row with the identity already on the owner's
     * account, so a first-time owner is not asked to retype their own name.
     */
    private ObjectNode defaultContractParty(User owner) {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        node.put("fullName", trimToEmpty(fullNameOf(owner)));
        node.putNull("gender");
        node.putNull("dateOfBirth");
        node.put("nationality", "");
        node.put("nationalId", "");
        node.put("currentAddress", "");
        node.put("phone", trimToEmpty(owner == null ? null : owner.getPhone()));
        node.put("occupation", "");
        return node;
    }

    private OwnerContractInfoResponse contractInfoFrom(JsonNode root) {
        JsonNode party = root == null ? null : root.get(CONTRACT_PARTY);
        if (party == null || !party.isObject()) {
            return emptyContractInfo();
        }
        return new OwnerContractInfoResponse(
                getTextOrDefault(party, "fullName", ""),
                genderOrNull(party, "gender"),
                dateOrNull(party, "dateOfBirth"),
                getTextOrDefault(party, "nationality", ""),
                getTextOrDefault(party, "nationalId", ""),
                getTextOrDefault(party, "currentAddress", ""),
                getTextOrDefault(party, "phone", ""),
                getTextOrDefault(party, "occupation", ""));
    }

    private static OwnerContractInfoResponse emptyContractInfo() {
        return new OwnerContractInfoResponse("", null, null, "", "", "", "", "");
    }

    private Gender genderOrNull(JsonNode node, String field) {
        String value = getTextOrDefault(node, field, "");
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Gender.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDate dateOrNull(JsonNode node, String field) {
        String value = getTextOrDefault(node, field, "");
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String fullNameOf(User user) {
        if (user == null) {
            return "";
        }
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        return (first + " " + last).trim();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private OwnerSettingsResponse toResponse(OwnerSetting setting) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(setting.getSettings());

            return new OwnerSettingsResponse(
                    setting.getId(),
                    setting.getOwner().getId(),
                    getTextOrDefault(root, "baseCurrency", "USD"),
                    getTextOrDefault(root, "displayCurrency", "USD"),
                    BigDecimal.valueOf(root.has("exchangeRate") ? root.get("exchangeRate").asDouble() : 4100.0),
                    getTextOrDefault(root, "dateFormat", "DD/MM/YYYY"),
                    getTextOrDefault(root, "timezone", "Asia/Phnom_Penh"),
                    getTextOrDefault(root, "businessName", ""),
                    getTextOrDefault(root, "logoUrl", ""),
                    getTextOrDefault(root, "phone", ""),
                    getTextOrDefault(root, "email", ""),
                    getTextOrDefault(root, "address", ""),
                    getTextOrDefault(root, "primaryColor", "#3b82f6"),
                    getTextOrDefault(root, "secondaryColor", "#10b981"),
                    getTextOrDefault(root, "documentLanguage", "KM"),
                    getTextOrDefault(root, "invoicePrefix", "INV"),
                    root.has("invoiceDefaultDueDays") ? root.get("invoiceDefaultDueDays").asInt(10) : 10,
                    getTextOrDefault(root, "invoiceFooter", ""),
                    root.has("invoiceShowUsd") ? root.get("invoiceShowUsd").asBoolean(true) : true,
                    root.has("invoiceShowKhr") ? root.get("invoiceShowKhr").asBoolean(true) : true,
                    root.has("invoiceShowLogo") ? root.get("invoiceShowLogo").asBoolean(true) : true,
                    root.has("invoiceShowBusinessInfo") ? root.get("invoiceShowBusinessInfo").asBoolean(true) : true,
                    getTextOrDefault(root, "contractTitle", "កិច្ចសន្យាជួលផ្ទះ"),
                    getTextOrDefault(root, "contractPaymentTerms", ""),
                    getTextOrDefault(root, "contractDepositTerms", ""),
                    getTextOrDefault(root, "contractTerminationTerms", ""),
                    getTextOrDefault(root, "contractAdditionalTerms", ""),
                    getTextOrDefault(root, "contractLanguage", "KM"),
                    contractInfoFrom(root),
                    getTextOrDefault(root, "receiptPrefix", "RCP"),
                    getTextOrDefault(root, "receiptLanguage", "KM"),
                    getTextOrDefault(root, "receiptFooter", ""),
                    root.has("receiptShowPaymentMethod") ? root.get("receiptShowPaymentMethod").asBoolean(true) : true,
                    root.has("receiptShowTransactionId") ? root.get("receiptShowTransactionId").asBoolean(true) : true,
                    root.has("receiptShowUsd") ? root.get("receiptShowUsd").asBoolean(true) : true,
                    root.has("receiptShowKhr") ? root.get("receiptShowKhr").asBoolean(true) : true,
                    getTextOrDefault(root, "telegramBotToken", ""),
                    getTextOrDefault(root, "telegramChatId", ""),
                    setting.getCreatedAt(),
                    setting.getUpdatedAt()
            );
        } catch (Exception e) {
            log.error("Failed to deserialize settings for owner: {}", setting.getOwner().getId(), e);
            throw new RuntimeException("Failed to read settings", e);
        }
    }

    private String getTextOrDefault(JsonNode node, String field, String defaultValue) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText(defaultValue);
        }
        return defaultValue;
    }
}
