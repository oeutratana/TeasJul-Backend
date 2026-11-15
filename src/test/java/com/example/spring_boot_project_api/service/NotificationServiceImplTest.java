package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.response.NotificationResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UnreadCountResponse;
import com.example.spring_boot_project_api.enums.NotificationType;
import com.example.spring_boot_project_api.exception.NotificationNotFoundException;
import com.example.spring_boot_project_api.mapper.NotificationMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.Listing;
import com.example.spring_boot_project_api.model.Notification;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.NotificationRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.impl.NotificationServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private UserRepository userRepository;
        @Mock private UserRoleRepository userRoleRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private ContractTenantRepository contractTenantRepository;
    @Mock private RentalContractRepository rentalContractRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private UserHouseAccessRepository userHouseAccessRepository;
    @Mock private jakarta.persistence.EntityManager entityManager;
    @Mock private SystemSettingService systemSettingService;
    @Mock private TelegramService telegramService;

    @InjectMocks private NotificationServiceImpl notificationService;

    private User me;

    @BeforeEach
    void setUp() {
        me = User.builder().id(1L).username("me").passwordHash("h").build();
        org.springframework.test.util.ReflectionTestUtils.setField(
                notificationService, "entityManager", entityManager);
        when(systemSettingService.isNotificationTypeEnabled(any())).thenReturn(true);
    }

    private Notification notificationOf(Long id, User owner) {
        return Notification.builder()
                .id(id)
                .user(owner)
                .type(NotificationType.INVOICE_CREATED)
                .title("t")
                .message("m")
                .data("{}")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ---- Ownership / IDOR prevention -------------------------------------

    @Test
    void markAsReadRejectsNotificationOwnedByAnotherUser() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        Notification otherOwned = notificationOf(10L, User.builder().id(2L).build());
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(otherOwned));

        assertThatThrownBy(() -> notificationService.markAsRead(10L))
                .isInstanceOf(NotificationNotFoundException.class);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsReadWorksForOwnedNotification() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        Notification mine = notificationOf(10L, me);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(mine));
        when(notificationMapper.toResponse(any(Notification.class)))
                .thenAnswer(inv -> new NotificationResponse(
                        ((Notification) inv.getArgument(0)).getId(),
                        NotificationType.INVOICE_CREATED, "t", "m",
                        Map.of("entity", "invoice", "id", 5L),
                        ((Notification) inv.getArgument(0)).getReadAt(),
                        LocalDateTime.now(), LocalDateTime.now()));

        NotificationResponse response = notificationService.markAsRead(10L);

        assertThat(response.readAt()).isNotNull();
        verify(notificationRepository).save(mine);
    }

    @Test
    void markAllAsReadScopedToCurrentUser() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        notificationService.markAllAsRead();

        verify(notificationRepository).markAllAsRead(1L);
    }

    @Test
    void getMyNotificationsOnlyReturnsCurrentUsersNotifications() {
        Pageable pageable = PageRequest.of(0, 10);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(notificationOf(10L, me)), pageable, 1));
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(
                new NotificationResponse(10L, NotificationType.INVOICE_CREATED, "t", "m",
                        Map.of(), null, LocalDateTime.now(), LocalDateTime.now()));

        PageResponse<NotificationResponse> result = notificationService.getMyNotifications(pageable);

        assertThat(result.content()).hasSize(1);
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq(1L), eq(pageable));
    }

    @Test
    void getUnreadCountUsesCurrentUser() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(notificationRepository.countByUserIdAndReadAtIsNull(1L)).thenReturn(3L);

        UnreadCountResponse result = notificationService.getUnreadCount();

        assertThat(result.unreadCount()).isEqualTo(3L);
    }

    // ---- Recipient resolution for business events ------------------------

    @Test
    void notifyPaymentSuccessTargetsTenantOwnerAndManager() {
        Long ownerId = 1L;
        Long managerId = 2L;
        Long tenantUserId = 3L;

        Payment payment = Payment.builder().id(50L).owner(me).invoiceId(100L)
                .paymentNo("PAY-1").amount(java.math.BigDecimal.TEN).build();
        Invoice invoice = Invoice.builder().id(100L).contractId(200L).build();
        RentalContract contract = RentalContract.builder().id(200L).ownerId(ownerId).roomId(20L).build();
        Room room = new Room();
        room.setId(20L);
        room.setHouseId(8L);
        Tenant tenant = Tenant.builder().id(90L).user(User.builder().id(tenantUserId).build()).build();
        ContractTenant link = ContractTenant.builder().id(1L).contractId(200L).tenantId(90L).build();

        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(rentalContractRepository.findById(200L)).thenReturn(Optional.of(contract));
        when(roomRepository.findById(20L)).thenReturn(Optional.of(room));
        when(userHouseAccessRepository.findByHouseId(8L))
                .thenReturn(List.of(com.example.spring_boot_project_api.model.UserHouseAccess.builder()
                        .user(User.builder().id(managerId).build()).build()));
        when(contractTenantRepository.findByContractId(200L)).thenReturn(List.of(link));
        when(tenantRepository.findById(90L)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(any())).thenAnswer(inv ->
                Optional.of(User.builder().id((Long) inv.getArgument(0)).build()));

        notificationService.notifyPaymentSuccess(payment);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(3)).saveAndFlush(captor.capture());
        List<Long> recipientIds = captor.getAllValues().stream()
                .map(Notification::getUser)
                .map(User::getId)
                .sorted()
                .toList();
        assertThat(recipientIds).containsExactly(1L, 2L, 3L);
        assertThat(captor.getAllValues().get(0).getType()).isEqualTo(NotificationType.PAYMENT_SUCCESS);
    }

    @Test
    void notifyRoomAssignedTargetsOnlyAssignedTenant() {
        Long tenantUserId = 8L;
        Tenant tenant = Tenant.builder().id(55L).user(User.builder().id(tenantUserId).build()).build();
        RentalContract contract = RentalContract.builder().id(300L).contractNo("C-300")
                .ownerId(1L).roomId(20L).build();
        Room room = new Room();
        room.setId(20L);
        room.setRoomNumber("B-2");
        when(roomRepository.findById(20L)).thenReturn(Optional.of(room));
        when(userRepository.findById(tenantUserId))
                .thenReturn(Optional.of(User.builder().id(tenantUserId).build()));

        notificationService.notifyRoomAssigned(contract, tenant);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUser().getId()).isEqualTo(tenantUserId);
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.ROOM_ASSIGNED);
    }

    @Test
    void notifyRoomAssignedSwallowsPersistenceFailureAndClearsContext() {
        Long tenantUserId = 8L;
        Tenant tenant = Tenant.builder().id(55L).user(User.builder().id(tenantUserId).build()).build();
        RentalContract contract = RentalContract.builder().id(300L).contractNo("C-300")
                .ownerId(1L).roomId(20L).build();
        Room room = new Room();
        room.setId(20L);
        room.setRoomNumber("B-2");
        when(roomRepository.findById(20L)).thenReturn(Optional.of(room));
        when(userRepository.findById(tenantUserId))
                .thenReturn(Optional.of(User.builder().id(tenantUserId).build()));
        when(notificationRepository.saveAndFlush(any(Notification.class)))
                .thenThrow(new RuntimeException("boom"));

        notificationService.notifyRoomAssigned(contract, tenant);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUser().getId()).isEqualTo(tenantUserId);
        verify(entityManager).clear();
    }

    @Test
    void notifyContractExpiringSkipsContractsOutsideNoticeWindow() {
        RentalContract farFuture = RentalContract.builder().id(400L)
                .endDate(java.time.LocalDate.now().plusDays(120)).build();

        notificationService.notifyContractExpiring(farFuture);

        verify(notificationRepository, never()).save(any());
    }

        @Test
        void notifyPropertySubmittedTargetsOnlySuperAdminsAndIncludesReference() {
                User superAdmin = User.builder().id(9L).build();
                Room room = new Room();
                room.setId(77L);
                room.setName("Modern Room");
                when(userRoleRepository.findUsersByRoleCode(SecurityUtils.ROLE_SUPER_ADMIN))
                                .thenReturn(List.of(superAdmin));
                when(userRepository.findById(9L)).thenReturn(Optional.of(superAdmin));
                when(notificationRepository.existsByUserIdAndTypeAndReferenceTypeAndReferenceId(
                        9L, NotificationType.PROPERTY_SUBMITTED, "ROOM", 77L)).thenReturn(false);

                notificationService.notifyPropertySubmitted(room);

                ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
                verify(notificationRepository).saveAndFlush(captor.capture());
                assertThat(captor.getValue().getUser().getId()).isEqualTo(9L);
                assertThat(captor.getValue().getType()).isEqualTo(NotificationType.PROPERTY_SUBMITTED);
                assertThat(captor.getValue().getReferenceType()).isEqualTo("ROOM");
                assertThat(captor.getValue().getReferenceId()).isEqualTo(77L);
                assertThat(captor.getValue().getData()).contains("referenceType", "ROOM", "referenceId", "77");
        }

        @Test
        void notifyPropertySubmittedDoesNotDuplicateExistingReference() {
                User superAdmin = User.builder().id(9L).build();
                Room room = new Room();
                room.setId(77L);
                when(userRoleRepository.findUsersByRoleCode(SecurityUtils.ROLE_SUPER_ADMIN))
                                .thenReturn(List.of(superAdmin));
                when(userRepository.findById(9L)).thenReturn(Optional.of(superAdmin));
                when(notificationRepository.existsByUserIdAndTypeAndReferenceTypeAndReferenceId(
                        9L, NotificationType.PROPERTY_SUBMITTED, "ROOM", 77L)).thenReturn(true);

                notificationService.notifyPropertySubmitted(room);

                verify(notificationRepository, never()).saveAndFlush(any(Notification.class));
        }

        @Test
        void notifyPropertySubmitted_skipsWhenDisabledInSystemSettings() {
                User superAdmin = User.builder().id(9L).build();
                Room room = new Room();
                room.setId(77L);
                when(userRoleRepository.findUsersByRoleCode(SecurityUtils.ROLE_SUPER_ADMIN))
                                .thenReturn(List.of(superAdmin));
                when(userRepository.findById(9L)).thenReturn(Optional.of(superAdmin));
                when(systemSettingService.isNotificationTypeEnabled(NotificationType.PROPERTY_SUBMITTED)).thenReturn(false);

                notificationService.notifyPropertySubmitted(room);

                verify(notificationRepository, never()).saveAndFlush(any(Notification.class));
        }

        // --- Super admin Telegram delivery on the existing PROPERTY_SUBMITTED hook ---

        @Test
        void notifyPropertySubmitted_sendsTelegramToSuperAdminForRoom() {
                User superAdmin = User.builder().id(9L).firstName("Sok").lastName("Owner").build();
                Room room = new Room();
                room.setId(77L);
                room.setName("Modern Room");
                room.setOwnerId(4L);
                when(userRoleRepository.findUsersByRoleCode(SecurityUtils.ROLE_SUPER_ADMIN))
                                .thenReturn(List.of(superAdmin));
                when(userRepository.findById(any())).thenReturn(Optional.of(superAdmin));

                notificationService.notifyPropertySubmitted(room);

                ArgumentCaptor<TelegramService.PropertySubmission> captor =
                                ArgumentCaptor.forClass(TelegramService.PropertySubmission.class);
                verify(telegramService).sendPropertySubmittedToSuperAdmin(captor.capture());
                assertThat(captor.getValue().reference()).isEqualTo("ROOM#77");
                assertThat(captor.getValue().propertyName()).isEqualTo("Modern Room");
                assertThat(captor.getValue().ownerName()).isEqualTo("Sok Owner");
        }

        @Test
        void notifyPropertySubmitted_sendsTelegramToSuperAdminForListing() {
                User superAdmin = User.builder().id(9L).build();
                Listing listing = new Listing();
                listing.setId(12L);
                listing.setTitle("Borey for rent");
                listing.setOwnerId(4L);
                when(userRoleRepository.findUsersByRoleCode(SecurityUtils.ROLE_SUPER_ADMIN))
                                .thenReturn(List.of(superAdmin));
                when(userRepository.findById(any())).thenReturn(Optional.of(superAdmin));

                notificationService.notifyPropertySubmitted(listing);

                ArgumentCaptor<TelegramService.PropertySubmission> captor =
                                ArgumentCaptor.forClass(TelegramService.PropertySubmission.class);
                verify(telegramService).sendPropertySubmittedToSuperAdmin(captor.capture());
                assertThat(captor.getValue().reference()).isEqualTo("PROPERTY#12");
                assertThat(captor.getValue().propertyName()).isEqualTo("Borey for rent");
        }

        @Test
        void notifyPropertySubmitted_telegramFailureDoesNotBreakTheInAppNotification() {
                User superAdmin = User.builder().id(9L).build();
                Room room = new Room();
                room.setId(77L);
                when(userRoleRepository.findUsersByRoleCode(SecurityUtils.ROLE_SUPER_ADMIN))
                                .thenReturn(List.of(superAdmin));
                when(userRepository.findById(any())).thenReturn(Optional.of(superAdmin));
                org.mockito.Mockito.doThrow(new IllegalStateException("telegram down"))
                                .when(telegramService).sendPropertySubmittedToSuperAdmin(any());

                assertThatCode(() -> notificationService.notifyPropertySubmitted(room))
                                .doesNotThrowAnyException();

                // The in-app notification is still created and persisted.
                verify(notificationRepository).saveAndFlush(any(Notification.class));
        }

        @Test
        void notifyPropertySubmitted_doesNotSendTelegramForAnUnsavedEntity() {
                notificationService.notifyPropertySubmitted((Room) null);

                verify(telegramService, never()).sendPropertySubmittedToSuperAdmin(any());
        }

        @Test
        void notifyPropertySubmitted_sendsTelegramEvenWhenInAppChannelIsOff() {
                // The two channels are independent apart from the shared system and
                // per-event switches, so an in-app-only outage must not mute Telegram.
                User superAdmin = User.builder().id(9L).build();
                Room room = new Room();
                room.setId(77L);
                when(userRoleRepository.findUsersByRoleCode(SecurityUtils.ROLE_SUPER_ADMIN))
                                .thenReturn(List.of(superAdmin));
                when(userRepository.findById(any())).thenReturn(Optional.of(superAdmin));
                when(systemSettingService.isNotificationTypeEnabled(NotificationType.PROPERTY_SUBMITTED)).thenReturn(false);

                notificationService.notifyPropertySubmitted(room);

                verify(notificationRepository, never()).saveAndFlush(any(Notification.class));
                verify(telegramService).sendPropertySubmittedToSuperAdmin(any());
        }
}