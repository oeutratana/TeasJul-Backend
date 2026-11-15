package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.NotificationResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UnreadCountResponse;
import com.example.spring_boot_project_api.enums.NotificationType;
import com.example.spring_boot_project_api.exception.NotificationNotFoundException;
import com.example.spring_boot_project_api.mapper.NotificationMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.Listing;
import com.example.spring_boot_project_api.model.MaintenanceRequest;
import com.example.spring_boot_project_api.model.Notification;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserHouseAccess;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.NotificationRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.NotificationService;
import com.example.spring_boot_project_api.service.TelegramService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

/**
 * In-app notifications. Recipients are ALWAYS resolved server-side from existing
 * business relationships (tenant {@code ->} user, contract owner, house managers),
 * never from the client request.
 *
 * <p>Event hooks are best-effort: a notification creation failure must never break
 * the underlying business operation (e.g. a confirmed payment being marked PAID).
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private static final int CONTRACT_EXPIRY_NOTICE_DAYS = 30;

    @PersistenceContext
    private EntityManager entityManager;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantRepository tenantRepository;
    private final ContractTenantRepository contractTenantRepository;
    private final RentalContractRepository rentalContractRepository;
    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final com.example.spring_boot_project_api.service.SystemSettingService systemSettingService;
    private final TelegramService telegramService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyPropertySubmitted(Room room) {
        try {
            if (room == null || room.getId() == null) {
                return;
            }
            String roomLabel = room.getName() != null && !room.getName().isBlank()
                    ? room.getName()
                    : room.getRoomNumber() != null ? "Room " + room.getRoomNumber() : "Room " + room.getId();
            String title = "New Property Submission";
            String message = String.format("Owner submitted %s for approval.", roomLabel);
            Map<String, Object> data = Map.of(
                    "entity", "room",
                    "id", room.getId(),
                    "referenceType", "ROOM",
                    "referenceId", room.getId());
            persistToAll(userIdsByRole(SecurityUtils.ROLE_SUPER_ADMIN),
                    NotificationType.PROPERTY_SUBMITTED, title, message, data);
            notifySuperAdminTelegram(new TelegramService.PropertySubmission(
                    "ROOM#" + room.getId(),
                    "Room / House",
                    roomLabel,
                    ownerName(room.getOwnerId() != null ? room.getOwnerId() : room.getSubmittedBy()),
                    room.getPublicationStatus() != null ? room.getPublicationStatus().name() : null,
                    room.getSubmittedAt()));
        } catch (RuntimeException e) {
            log.warn("Failed to create property-submitted notification: {}", e.getClass().getSimpleName());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyPropertySubmitted(Listing listing) {
        try {
            if (listing == null || listing.getId() == null) {
                return;
            }
            String title = "New Property Submission";
            String message = String.format("Owner submitted property listing #%s for approval.", listing.getId());
            Map<String, Object> data = Map.of(
                    "entity", "listing",
                    "id", listing.getId(),
                    "referenceType", "PROPERTY",
                    "referenceId", listing.getId());
            persistToAll(userIdsByRole(SecurityUtils.ROLE_SUPER_ADMIN),
                    NotificationType.PROPERTY_SUBMITTED, title, message, data);
            notifySuperAdminTelegram(new TelegramService.PropertySubmission(
                    "PROPERTY#" + listing.getId(),
                    "Property",
                    listing.getTitle(),
                    ownerName(listing.getOwnerId() != null ? listing.getOwnerId() : listing.getSubmittedBy()),
                    listing.getPublicationStatus() != null ? listing.getPublicationStatus().name() : null,
                    listing.getSubmittedAt()));
        } catch (RuntimeException e) {
            log.warn("Failed to create property-submitted notification: {}", e.getClass().getSimpleName());
        }
    }

    /**
     * Sends the same PROPERTY_SUBMITTED event to the SUPER_ADMIN Telegram
     * destination. The existing event hook is the single dispatch point: this
     * adds a channel, not a second event. Isolated in its own try/catch so a
     * Telegram outage can never break the in-app notification or the submission
     * transaction that triggered it.
     */
    private void notifySuperAdminTelegram(TelegramService.PropertySubmission submission) {
        try {
            telegramService.sendPropertySubmittedToSuperAdmin(submission);
        } catch (RuntimeException e) {
            log.warn("Failed to send property-submitted Telegram notification: {}",
                    e.getClass().getSimpleName());
        }
    }

    /** Best-effort owner display name for the Telegram message. */
    private String ownerName(Long ownerUserId) {
        if (ownerUserId == null) {
            return null;
        }
        try {
            return userRepository.findById(ownerUserId)
                    .map(user -> {
                        String name = (user.getFirstName() != null ? user.getFirstName() : "")
                                + (user.getLastName() != null ? " " + user.getLastName() : "");
                        name = name.trim();
                        return name.isEmpty() ? user.getUsername() : name;
                    })
                    .orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---- Reads (owner-scoped by the authenticated user) ------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<NotificationResponse> content = page.getContent().stream()
                .map(notificationMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        long count = notificationRepository
                .countByUserIdAndReadAtIsNull(securityUtils.getCurrentUserId());
        return new UnreadCountResponse(count);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findById(id)
                .filter(n -> n.getUser().getId().equals(userId))
                .orElseThrow(() -> NotificationNotFoundException.forId(id));

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllAsRead(securityUtils.getCurrentUserId());
    }

    // ---- Event hooks -----------------------------------------------------

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyInvoiceCreated(Invoice invoice) {
        try {
            if (invoice == null) {
                return;
            }
            BigDecimal total = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
            String title = "New invoice ready";
            String message = String.format("Invoice %s (%s) has been issued. Total due: $%s",
                    invoice.getInvoiceNo(), billingPeriod(invoice.getBillingYear(), invoice.getBillingMonth()), total);
            Map<String, Object> data = Map.of("entity", "invoice", "id", invoice.getId());

            persistToAll(tenantUserIdsByInvoice(invoice),
                    NotificationType.INVOICE_CREATED, title, message, data);
        } catch (RuntimeException e) {
            log.warn("Failed to create invoice notification: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyPaymentCreated(Payment payment) {
        try {
            if (payment == null || payment.getInvoiceId() == null) {
                return;
            }
            String title = "Payment initiated";
            String message = String.format("Your payment of $%s is being processed (ref %s).",
                    payment.getAmount(), payment.getPaymentNo());
            Map<String, Object> data = Map.of("entity", "payment", "id", payment.getId());

            Invoice invoice = invoiceRepository.findById(payment.getInvoiceId()).orElse(null);
            persistToAll(tenantUserIdsByInvoice(invoice),
                    NotificationType.PAYMENT_CREATED, title, message, data);
        } catch (RuntimeException e) {
            log.warn("Failed to create payment-created notification: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyPaymentSuccess(Payment payment) {
        try {
            if (payment == null) {
                return;
            }
            String title = "Payment successful";
            String message = String.format("Your payment of $%s was confirmed (ref %s).",
                    payment.getAmount(), payment.getPaymentNo());
            Map<String, Object> data = Map.of("entity", "payment", "id", payment.getId());

            Set<Long> recipients = new LinkedHashSet<>();
            Invoice invoice = payment.getInvoiceId() != null
                    ? invoiceRepository.findById(payment.getInvoiceId()).orElse(null)
                    : null;
            if (invoice != null) {
                recipients.addAll(tenantUserIdsByInvoice(invoice));
                rentalContractRepository.findById(invoice.getContractId())
                        .ifPresent(contract -> recipients.addAll(ownerAndManagerUserIds(contract)));
            }
            if (payment.getOwner() != null) {
                recipients.add(payment.getOwner().getId());
            }

            persistToAll(recipients, NotificationType.PAYMENT_SUCCESS, title, message, data);
        } catch (RuntimeException e) {
            log.warn("Failed to create payment-success notification: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyPaymentFailed(Payment payment) {
        try {
            if (payment == null || payment.getInvoiceId() == null) {
                return;
            }
            String title = "Payment failed";
            String message = String.format("Your payment of $%s could not be completed (ref %s). Please try again.",
                    payment.getAmount(), payment.getPaymentNo());
            Map<String, Object> data = Map.of("entity", "payment", "id", payment.getId());

            Invoice invoice = invoiceRepository.findById(payment.getInvoiceId()).orElse(null);
            persistToAll(tenantUserIdsByInvoice(invoice),
                    NotificationType.PAYMENT_FAILED, title, message, data);
        } catch (RuntimeException e) {
            log.warn("Failed to create payment-failed notification: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyMaintenanceRequestCreated(MaintenanceRequest request) {
        try {
            if (request == null) {
                return;
            }
            String title = "New maintenance request";
            String message = String.format("%s — %s (%s)", request.getRequestNo(),
                    request.getTitle(), request.getStatus());
            Map<String, Object> data = Map.of("entity", "maintenance", "id", request.getId());

            persistToAll(maintenanceRecipientIds(request),
                    NotificationType.MAINTENANCE_REQUEST, title, message, data);
        } catch (RuntimeException e) {
            log.warn("Failed to create maintenance-created notification: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyMaintenanceStatusChanged(MaintenanceRequest request) {
        try {
            if (request == null) {
                return;
            }
            String title = "Maintenance request updated";
            String message = String.format("%s is now %s.", request.getRequestNo(), request.getStatus());
            Map<String, Object> data = Map.of("entity", "maintenance", "id", request.getId());

            persistToAll(maintenanceRecipientIds(request),
                    NotificationType.MAINTENANCE_STATUS, title, message, data);
        } catch (RuntimeException e) {
            log.warn("Failed to create maintenance-status notification: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyContractExpiring(RentalContract contract) {
        try {
            if (contract == null || contract.getEndDate() == null) {
                return;
            }
            LocalDate today = LocalDate.now();
            if (contract.getEndDate().isBefore(today)
                    || contract.getEndDate().isAfter(today.plusDays(CONTRACT_EXPIRY_NOTICE_DAYS))) {
                return;
            }
            String title = "Contract expiring soon";
            String message = String.format("Contract %s expires on %s.",
                    contract.getContractNo(), contract.getEndDate());
            Map<String, Object> data = Map.of("entity", "contract", "id", contract.getId());

            Set<Long> recipients = new LinkedHashSet<>(ownerAndManagerUserIds(contract));
            recipients.addAll(tenantUserIdsByContract(contract.getId()));

            persistToAll(recipients, NotificationType.CONTRACT_EXPIRING, title, message, data);
        } catch (RuntimeException e) {
            log.warn("Failed to create contract-expiry notification: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyRoomAssigned(RentalContract contract, Tenant tenant) {
        try {
            if (contract == null || tenant == null || tenant.getUser() == null) {
                return;
            }
            Room room = contract.getRoomId() != null
                    ? roomRepository.findById(contract.getRoomId()).orElse(null)
                    : null;
            String roomLabel = room != null ? room.getRoomNumber() : String.valueOf(contract.getRoomId());
            String title = "Room assigned";
            String message = String.format("You have been assigned to room %s (contract %s).",
                    roomLabel, contract.getContractNo());
            Map<String, Object> data = Map.of("entity", "contract", "id", contract.getId());

            persist(tenant.getUser().getId(), NotificationType.ROOM_ASSIGNED, title, message, data);
        } catch (RuntimeException e) {
            log.warn("Failed to create room-assigned notification: {}", e.getMessage());
        }
    }

    // ---- Recipient resolution (server-side, from real relationships) ----

    private Set<Long> tenantUserIdsByInvoice(Invoice invoice) {
        if (invoice == null) {
            return Set.of();
        }
        return tenantUserIdsByContract(invoice.getContractId());
    }

    private Set<Long> userIdsByRole(String roleCode) {
        return userRoleRepository.findUsersByRoleCode(roleCode).stream()
                .map(User::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> tenantUserIdsByContract(Long contractId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (ContractTenant link : contractTenantRepository.findByContractId(contractId)) {
            tenantRepository.findById(link.getTenantId())
                    .filter(t -> t.getUser() != null)
                    .map(t -> t.getUser().getId())
                    .ifPresent(ids::add);
        }
        return ids;
    }

    private Set<Long> ownerAndManagerUserIds(RentalContract contract) {
        Set<Long> ids = new LinkedHashSet<>();
        if (contract.getOwnerId() != null) {
            ids.add(contract.getOwnerId());
        }
        Room room = contract.getRoomId() != null
                ? roomRepository.findById(contract.getRoomId()).orElse(null)
                : null;
        if (room != null) {
            for (UserHouseAccess access : userHouseAccessRepository.findByHouseId(room.getHouseId())) {
                ids.add(access.getUser().getId());
            }
        }
        return ids;
    }

    private Set<Long> maintenanceRecipientIds(MaintenanceRequest request) {
        Set<Long> ids = new LinkedHashSet<>();
        User owner = request.getOwner();
        if (owner != null) {
            ids.add(owner.getId());
        }
        if (request.getHouse() != null) {
            ids.add(request.getHouse().getOwner().getId());
            for (UserHouseAccess access : userHouseAccessRepository
                    .findByHouseId(request.getHouse().getId())) {
                ids.add(access.getUser().getId());
            }
        }
        if (request.getTenant() != null && request.getTenant().getUser() != null) {
            ids.add(request.getTenant().getUser().getId());
        }
        return ids;
    }

    // ---- Helpers ----------------------------------------------------------

    /**
     * Persists a notification for one user, skipping inactive/missing accounts.
     * Dedupes when the same user is targeted via multiple relationships.
     */
    private void persist(Long userId, NotificationType type, String title,
            String message, Map<String, Object> data) {
        if (userId == null) {
            return;
        }
        if (systemSettingService != null && !systemSettingService.isNotificationTypeEnabled(type)) {
            log.debug("Notification type {} is disabled by system settings; skipping for user {}", type, userId);
            return;
        }
        userRepository.findById(userId).ifPresent(user -> {
            String referenceType = data == null ? null : String.valueOf(data.get("referenceType"));
            Object referenceValue = data == null ? null : data.get("referenceId");
            Long referenceId = referenceId(referenceValue);
            if (referenceType != null && !"null".equals(referenceType) && referenceId != null
                && notificationRepository.existsByUserIdAndTypeAndReferenceTypeAndReferenceId(
                    userId, type, referenceType, referenceId)) {
                return;
            }
            Notification notification = Notification.builder()
                    .user(user)
                    .type(type)
                    .title(title)
                    .message(message)
                    .data(toJson(data))
                .referenceType(referenceType)
                .referenceId(referenceId)
                    .build();
            try {
                notificationRepository.saveAndFlush(notification);
            } catch (RuntimeException e) {
                log.warn("Failed to create {} notification for user {}: {}",
                        type, userId, e.getMessage());
                entityManager.clear();
            }
        });
    }

    private String toJson(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Long referenceId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return Long.valueOf(number.longValue());
        }
        return Long.valueOf(String.valueOf(value));
    }

    private void persistToAll(Collection<Long> userIds, NotificationType type, String title,
            String message, Map<String, Object> data) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        new LinkedHashSet<>(userIds)
                .forEach(uid -> persist(uid, type, title, message, data));
    }

    private String billingPeriod(Integer year, Integer month) {
        if (year == null || month == null) {
            return "unknown period";
        }
        return YearMonth.of(year, month).toString();
    }
}