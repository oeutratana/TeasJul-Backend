package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.spring_boot_project_api.config.TelegramBotConfig;
import com.example.spring_boot_project_api.enums.NotificationType;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.PaymentMethod;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.service.OwnerSettingService;
import com.example.spring_boot_project_api.service.SuperAdminTelegramSettings;
import com.example.spring_boot_project_api.service.TelegramService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelegramServiceImpl implements TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramServiceImpl.class);
    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RestTemplate restTemplate;
    private final TelegramBotConfig telegramBotConfig;
    private final OwnerSettingService ownerSettingService;
    private final InvoiceRepository invoiceRepository;
    private final RentalContractRepository rentalContractRepository;
    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final ContractTenantRepository contractTenantRepository;
    private final TenantRepository tenantRepository;
    private final com.example.spring_boot_project_api.service.SystemSettingService systemSettingService;

    @Override
    public void sendPaymentCreated(Payment payment) {
        try {
            String message = "🆕 New payment initiated\n" + baseDetails(payment) + refLine(payment);
            send(payment, message);
        } catch (RuntimeException e) {
            log.warn("Failed to send Telegram payment-created message: {}", e.getMessage());
        }
    }

    @Override
    public void sendPaymentSuccess(Payment payment) {
        try {
            String message = "✅ Payment successful\n" + baseDetails(payment) + refLine(payment)
                    + "Time: " + timestamp(payment) + "\n";
            send(payment, message);
        } catch (RuntimeException e) {
            log.warn("Failed to send Telegram payment-success message: {}", e.getMessage());
        }
    }

    @Override
    public void sendPropertySubmittedToSuperAdmin(PropertySubmission submission) {
        if (submission == null) {
            return;
        }
        try {
            if (!systemSettingService.isTelegramNotificationEnabledFor(NotificationType.PROPERTY_SUBMITTED)) {
                log.debug("Telegram notifications for PROPERTY_SUBMITTED are disabled; skipping message");
                return;
            }
            SuperAdminTelegramSettings settings = systemSettingService.getSuperAdminTelegramSettingsValues();
            if (!settings.isDeliverable()) {
                log.debug("Super admin Telegram destination is not configured; skipping property-submitted message");
                return;
            }
            post(settings.botToken(), settings.chatId(), propertySubmittedMessage(submission));
            log.info("Sent Telegram property-submitted notification to the super admin destination");
        } catch (RuntimeException e) {
            // Log the failure type only: the exception message can embed the request
            // URL, which carries the bot token.
            log.warn("Failed to send Telegram property-submitted message: {}",
                    e.getClass().getSimpleName());
        }
    }

    private String propertySubmittedMessage(PropertySubmission submission) {
        StringBuilder sb = new StringBuilder("🏠 New property submitted for approval\n");
        appendLine(sb, "Property", submission.propertyName());
        appendLine(sb, "Type", submission.propertyType());
        appendLine(sb, "Owner", submission.ownerName());
        appendLine(sb, "Status", submission.status());
        appendLine(sb, "Submitted", submission.submittedAt() != null
                ? submission.submittedAt().format(TIME_FORMATTER)
                : "—");
        appendLine(sb, "Reference", submission.reference());
        return sb.toString();
    }

    private void appendLine(StringBuilder sb, String label, String value) {
        sb.append(label).append(": ").append(value == null || value.isBlank() ? "—" : value).append('\n');
    }

    /**
     * Posts a message to an explicit bot token / chat id pair, mirroring the
     * owner payment path.
     */
    private void post(String botToken, String chatId, String text) {
        String url = TELEGRAM_API_BASE + botToken + "/sendMessage";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> payload = Map.of("chat_id", chatId, "text", text);
        restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
    }

    private void send(Payment payment, String text) {
        if (systemSettingService != null && !systemSettingService.isTelegramNotificationEnabled()) {
            log.debug("Telegram notifications are disabled by system settings; skipping");
            return;
        }
        Long ownerId = resolveOwnerId(payment);
        String[] credentials = resolveCredentials(ownerId);
        if (credentials == null) {
            log.debug("Telegram bot not configured (ownerId={}); skipping message", ownerId);
            return;
        }

        String url = TELEGRAM_API_BASE + credentials[0] + "/sendMessage";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> payload = Map.of("chat_id", credentials[1], "text", text);
        restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
        log.info("Sent Telegram payment notification (ownerId={})", ownerId);
    }

    private String[] resolveCredentials(Long ownerId) {
        String token = ownerSettingService.getTelegramBotToken(ownerId);
        if (isBlank(token)) {
            token = telegramBotConfig.getBotToken();
        }
        String chatId = ownerSettingService.getTelegramChatId(ownerId);
        if (isBlank(chatId)) {
            chatId = telegramBotConfig.getChatId();
        }
        if (isBlank(token) || isBlank(chatId)) {
            return null;
        }
        return new String[] { token, chatId };
    }

    private String baseDetails(Payment payment) {
        PaymentSummary summary = resolveSummary(payment);
        BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
        BigDecimal khr = khrEquivalent(amount, payment.getExchangeRate());
        StringBuilder sb = new StringBuilder();
        if (summary.invoiceNo() != null) {
            sb.append("Invoice: ").append(summary.invoiceNo()).append('\n');
        }
        if (summary.tenantName() != null) {
            sb.append("Tenant: ").append(summary.tenantName()).append('\n');
        }
        if (summary.houseName() != null && summary.roomNumber() != null) {
            sb.append("House: ").append(summary.houseName()).append(" (Room ").append(summary.roomNumber()).append(")\n");
        }
        sb.append("Amount: $").append(amount.setScale(2, RoundingMode.HALF_UP));
        if (khr != null) {
            sb.append(" (").append(khr.toPlainString()).append(" KHR)");
        }
        sb.append('\n');
        sb.append("Method: ").append(methodName(payment)).append('\n');
        return sb.toString();
    }

    private String refLine(Payment payment) {
        if (payment.getPaymentNo() != null) {
            return "Ref: " + payment.getPaymentNo() + "\n";
        }
        if (payment.getTransactionId() != null) {
            return "Ref: " + payment.getTransactionId() + "\n";
        }
        return "";
    }

    private String methodName(Payment payment) {
        PaymentMethod method = payment.getPaymentMethod();
        return method != null && method.getName() != null ? method.getName() : "—";
    }

    private BigDecimal khrEquivalent(BigDecimal amount, BigDecimal exchangeRate) {
        if (exchangeRate == null) {
            return null;
        }
        return amount.multiply(exchangeRate).setScale(0, RoundingMode.HALF_UP);
    }

    private String timestamp(Payment payment) {
        LocalDateTime paidAt = payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt();
        return paidAt != null ? paidAt.format(TIME_FORMATTER) : "—";
    }

    private Long resolveOwnerId(Payment payment) {
        if (payment.getOwner() != null && payment.getOwner().getId() != null) {
            return payment.getOwner().getId();
        }
        if (payment.getInvoiceId() != null) {
            return invoiceRepository.findById(payment.getInvoiceId())
                    .map(i -> i.getOwner())
                    .map(com.example.spring_boot_project_api.model.User::getId)
                    .orElse(null);
        }
        return null;
    }

    private PaymentSummary resolveSummary(Payment payment) {
        if (payment.getInvoiceId() == null) {
            return new PaymentSummary(null, null, null, null);
        }
        Invoice invoice = invoiceRepository.findById(payment.getInvoiceId()).orElse(null);
        if (invoice == null) {
            return new PaymentSummary(null, null, null, null);
        }
        String invoiceNo = invoice.getInvoiceNo();

        RentalContract contract = invoice.getContractId() != null
                ? rentalContractRepository.findById(invoice.getContractId()).orElse(null)
                : null;
        if (contract == null) {
            return new PaymentSummary(invoiceNo, null, null, null);
        }

        Room room = contract.getRoomId() != null
                ? roomRepository.findById(contract.getRoomId()).orElse(null)
                : null;
        House house = room != null && room.getHouseId() != null
                ? houseRepository.findById(room.getHouseId()).orElse(null)
                : null;

        String tenantName = primaryTenantName(contract.getId());

        return new PaymentSummary(
                invoiceNo,
                tenantName,
                house != null ? house.getName() : null,
                room != null ? room.getRoomNumber() : null);
    }

    private String primaryTenantName(Long contractId) {
        ContractTenant chosen = null;
        for (ContractTenant link : contractTenantRepository.findByContractId(contractId)) {
            if (Boolean.TRUE.equals(link.getIsPrimary())) {
                chosen = link;
                break;
            }
        }
        if (chosen == null) {
            chosen = contractTenantRepository.findByContractId(contractId).stream()
                    .min((a, b) -> {
                        int c = a.getJoinedAt().compareTo(b.getJoinedAt());
                        return c != 0 ? c : a.getId().compareTo(b.getId());
                    })
                    .orElse(null);
        }
        if (chosen == null) {
            return null;
        }
        Tenant tenant = tenantRepository.findById(chosen.getTenantId()).orElse(null);
        if (tenant == null) {
            return null;
        }
        String first = tenant.getFirstName() != null ? tenant.getFirstName() : "";
        String last = tenant.getLastName() != null ? tenant.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isEmpty() ? (tenant.getEmail() != null ? tenant.getEmail() : null) : name;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record PaymentSummary(String invoiceNo, String tenantName, String houseName, String roomNumber) {
    }
}