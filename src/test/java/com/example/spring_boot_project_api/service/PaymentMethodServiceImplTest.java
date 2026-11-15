package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.CreatePaymentMethodRequest;
import com.example.spring_boot_project_api.dto.request.UpdatePaymentMethodRequest;
import com.example.spring_boot_project_api.enums.PaymentMethodStatus;
import com.example.spring_boot_project_api.exception.InvalidPaymentOperationException;
import com.example.spring_boot_project_api.exception.PaymentMethodNotFoundException;
import com.example.spring_boot_project_api.model.PaymentMethod;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.PaymentMethodRepository;
import com.example.spring_boot_project_api.service.impl.PaymentMethodServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceImplTest {

    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private HouseAccessService houseAccessService;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks private PaymentMethodServiceImpl paymentMethodService;

    @Test
    void listPaymentMethods_owner_returns_mapped_methods() {
        User owner = User.builder().id(1L).build();
        PaymentMethod aba = PaymentMethod.builder()
                .id(1L).owner(owner).name("ABA KhQR").code("KHQR")
                .description("ABA Bank KHQR").status(PaymentMethodStatus.ACTIVE).build();
        PaymentMethod cash = PaymentMethod.builder()
                .id(2L).owner(owner).name("Cash").code("CASH")
                .status(PaymentMethodStatus.ACTIVE).build();

        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(1L));
        when(paymentMethodRepository.findByOwnerIdInOrderByIdAsc(List.of(1L)))
                .thenReturn(List.of(aba, cash));

        var result = paymentMethodService.listPaymentMethods();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("KHQR");
        assertThat(result.get(0).description()).isEqualTo("ABA Bank KHQR");
        assertThat(result.get(1).name()).isEqualTo("Cash");
    }

    @Test
    void listPaymentMethods_no_access_returns_empty() {
        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of());

        var result = paymentMethodService.listPaymentMethods();

        assertThat(result).isEmpty();
    }

    @Test
    void createPaymentMethod_owner_success() {
        User owner = User.builder().id(1L).username("owner1").build();
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(paymentMethodRepository.existsByOwnerIdAndCode(1L, "CASH")).thenReturn(false);
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = paymentMethodService.createPaymentMethod(
                new CreatePaymentMethodRequest("Cash", "cash", "រូបិយប័ណ្ណផ្ទាល់"));

        assertThat(result.name()).isEqualTo("Cash");
        assertThat(result.code()).isEqualTo("CASH");
        assertThat(result.description()).isEqualTo("រូបិយប័ណ្ណផ្ទាល់");
        assertThat(result.status()).isEqualTo(PaymentMethodStatus.ACTIVE);
    }

    @Test
    void createPaymentMethod_duplicate_code_rejected() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(paymentMethodRepository.existsByOwnerIdAndCode(1L, "CASH")).thenReturn(true);

        assertThatThrownBy(() -> paymentMethodService.createPaymentMethod(
                new CreatePaymentMethodRequest("Cash", "cash", null)))
                .isInstanceOf(InvalidPaymentOperationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createPaymentMethod_manager_denied() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        assertThatThrownBy(() -> paymentMethodService.createPaymentMethod(
                new CreatePaymentMethodRequest("Cash", "CASH", null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updatePaymentMethod_owner_success() {
        User owner = User.builder().id(1L).username("owner1").build();
        PaymentMethod method = PaymentMethod.builder()
                .id(1L).owner(owner).name("Cash").code("CASH")
                .status(PaymentMethodStatus.ACTIVE).build();

        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(paymentMethodRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(method));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = paymentMethodService.updatePaymentMethod(1L,
                new UpdatePaymentMethodRequest("Cash 2", "updated", PaymentMethodStatus.INACTIVE));

        assertThat(result.name()).isEqualTo("Cash 2");
        assertThat(result.description()).isEqualTo("updated");
        assertThat(result.status()).isEqualTo(PaymentMethodStatus.INACTIVE);
    }

    @Test
    void updatePaymentMethod_not_owner_not_found() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(paymentMethodRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentMethodService.updatePaymentMethod(99L,
                new UpdatePaymentMethodRequest("Cash", null, PaymentMethodStatus.ACTIVE)))
                .isInstanceOf(PaymentMethodNotFoundException.class);
    }
}