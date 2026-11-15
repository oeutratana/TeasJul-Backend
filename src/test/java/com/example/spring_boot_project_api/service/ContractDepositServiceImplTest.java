package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.CreateDepositRequest;
import com.example.spring_boot_project_api.dto.request.UpdateDepositPaymentRequest;
import com.example.spring_boot_project_api.dto.response.ContractDepositResponse;
import com.example.spring_boot_project_api.enums.ContractDepositStatus;
import com.example.spring_boot_project_api.model.ContractDeposit;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.repository.ContractDepositRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.service.impl.ContractDepositServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class ContractDepositServiceImplTest {

    @Mock
    private ContractDepositRepository depositRepository;
    @Mock
    private RentalContractRepository contractRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private HouseAccessService houseAccessService;

    @InjectMocks
    private ContractDepositServiceImpl depositService;

    private RentalContract contract() {
        return RentalContract.builder().id(1L).ownerId(10L).roomId(5L).build();
    }

    private void stubOwnedContract() {
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract()));
    }

    private void stubSaveReturnsArgument() {
        when(depositRepository.save(any(ContractDeposit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createDepositWithAlreadyPaidStatusPersistsFullPaidAmountAndPaidStatus() {
        stubOwnedContract();
        stubSaveReturnsArgument();

        CreateDepositRequest request = new CreateDepositRequest(
                1L, new BigDecimal("150.00"), null, ContractDepositStatus.PAID,
                LocalDate.of(2026, 9, 19));

        ContractDepositResponse response = depositService.createDeposit(request);

        assertThat(response.amount()).isEqualByComparingTo("150.00");
        assertThat(response.paidAmount()).isEqualByComparingTo("150.00");
        assertThat(response.status()).isEqualTo(ContractDepositStatus.PAID);
        assertThat(response.paidAt()).isNotNull();
        assertThat(response.refundedAt()).isNull();
        verify(depositRepository).save(any(ContractDeposit.class));
    }

    @Test
    void createDepositWithPendingStatusPersistsZeroPaidAndPendingStatus() {
        stubOwnedContract();
        stubSaveReturnsArgument();

        CreateDepositRequest request = new CreateDepositRequest(
                1L, new BigDecimal("150.00"), null, ContractDepositStatus.PENDING,
                LocalDate.of(2026, 9, 19));

        ContractDepositResponse response = depositService.createDeposit(request);

        assertThat(response.amount()).isEqualByComparingTo("150.00");
        assertThat(response.paidAmount()).isEqualByComparingTo("0");
        assertThat(response.status()).isEqualTo(ContractDepositStatus.PENDING);
        assertThat(response.paidAt()).isNull();
        assertThat(response.refundedAt()).isNull();
    }

    @Test
    void createDepositDefaultsToPendingWhenStatusIsNull() {
        stubOwnedContract();
        stubSaveReturnsArgument();

        CreateDepositRequest request = new CreateDepositRequest(
                1L, new BigDecimal("150.00"), null, null, null);

        ContractDepositResponse response = depositService.createDeposit(request);

        assertThat(response.status()).isEqualTo(ContractDepositStatus.PENDING);
        assertThat(response.paidAmount()).isEqualByComparingTo("0");
        assertThat(response.paidAt()).isNull();
    }

    @Test
    void createDepositWithAlreadyPaidAndNoDateDefaultsPaidAtToNow() {
        stubOwnedContract();
        stubSaveReturnsArgument();

        CreateDepositRequest request = new CreateDepositRequest(
                1L, new BigDecimal("150.00"), null, ContractDepositStatus.PAID, null);

        ContractDepositResponse response = depositService.createDeposit(request);

        assertThat(response.status()).isEqualTo(ContractDepositStatus.PAID);
        assertThat(response.paidAmount()).isEqualByComparingTo("150.00");
        assertThat(response.paidAt()).isNotNull();
    }

    @Test
    void createDepositWithAlreadyPaidPersistsDateOnlyAsStartOfDay() {
        stubOwnedContract();
        stubSaveReturnsArgument();

        CreateDepositRequest request = new CreateDepositRequest(
                1L, new BigDecimal("200.00"), null, ContractDepositStatus.PAID,
                LocalDate.of(2026, 9, 19));

        ContractDepositResponse response = depositService.createDeposit(request);

        assertThat(response.paidAt()).isEqualTo(java.time.LocalDateTime.of(2026, 9, 19, 0, 0));
    }

    @Test
    void createDepositIgnoresPaidDateWhenRequestedStatusIsPending() {
        stubOwnedContract();
        stubSaveReturnsArgument();

        CreateDepositRequest request = new CreateDepositRequest(
                1L, new BigDecimal("150.00"), null, ContractDepositStatus.PENDING,
                LocalDate.of(2026, 9, 19));

        ContractDepositResponse response = depositService.createDeposit(request);

        assertThat(response.status()).isEqualTo(ContractDepositStatus.PENDING);
        assertThat(response.paidAt()).isNull();
    }

    @Test
    void markAsPaidTransitionsPendingToPaidWithGivenAmount() {
        ContractDeposit deposit = ContractDeposit.builder()
                .id(7L)
                .rentalContract(contract())
                .amount(new BigDecimal("150.00"))
                .paidAmount(BigDecimal.ZERO)
                .status(ContractDepositStatus.PENDING)
                .build();
        when(depositRepository.findById(7L)).thenReturn(Optional.of(deposit));
        when(depositRepository.save(any(ContractDeposit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContractDepositResponse response = depositService.markAsPaid(
                7L, new UpdateDepositPaymentRequest(new BigDecimal("150.00")));

        assertThat(response.status()).isEqualTo(ContractDepositStatus.PAID);
        assertThat(response.paidAmount()).isEqualByComparingTo("150.00");
        assertThat(response.paidAt()).isNotNull();
    }

    @Test
    void getDepositByIdNormalizesNullPaidAmountAndNullStatus() {
        ContractDeposit deposit = ContractDeposit.builder()
                .id(7L)
                .rentalContract(contract())
                .amount(new BigDecimal("150.00"))
                .paidAmount(null)
                .status(null)
                .build();
        when(depositRepository.findById(7L)).thenReturn(Optional.of(deposit));

        ContractDepositResponse response = depositService.getDepositById(7L);

        assertThat(response.paidAmount()).isEqualByComparingTo("0");
        assertThat(response.status()).isEqualTo(ContractDepositStatus.PENDING);
    }

    @Test
    void getDepositsByContractReturnsMappedList() {
        ContractDeposit paid = ContractDeposit.builder()
                .id(1L).rentalContract(contract()).amount(new BigDecimal("150.00"))
                .paidAmount(new BigDecimal("150.00")).status(ContractDepositStatus.PAID).build();
        ContractDeposit pending = ContractDeposit.builder()
                .id(2L).rentalContract(contract()).amount(new BigDecimal("100.00"))
                .paidAmount(BigDecimal.ZERO).status(ContractDepositStatus.PENDING).build();
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract()));
        when(depositRepository.findByRentalContractId(1L)).thenReturn(List.of(paid, pending));

        List<ContractDepositResponse> responses = depositService.getDepositsByContract(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).status()).isEqualTo(ContractDepositStatus.PAID);
        assertThat(responses.get(0).paidAmount()).isEqualByComparingTo("150.00");
        assertThat(responses.get(1).status()).isEqualTo(ContractDepositStatus.PENDING);
        assertThat(responses.get(1).paidAmount()).isEqualByComparingTo("0");
    }

    @Test
    void ownerCannotAccessAnotherOwnersDeposit() {
        ContractDeposit deposit = ContractDeposit.builder()
                .id(7L).rentalContract(contract()).amount(new BigDecimal("150.00"))
                .paidAmount(new BigDecimal("150.00")).status(ContractDepositStatus.PAID).build();
        when(depositRepository.findById(7L)).thenReturn(Optional.of(deposit));
        when(houseAccessService.resolveContractHouseId(anyLong()))
                .thenThrow(new AccessDeniedException("You do not have permission to access this resource"));

        assertThatThrownBy(() -> depositService.getDepositById(7L))
                .isInstanceOf(AccessDeniedException.class);
    }
}