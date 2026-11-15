package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.CreateRenewalRequest;
import com.example.spring_boot_project_api.dto.response.ContractRenewalResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.exception.RentalContractNotFoundException;
import com.example.spring_boot_project_api.mapper.ContractRenewalMapper;
import com.example.spring_boot_project_api.model.ContractRenewal;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractRenewalRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.service.impl.ContractRenewalServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class ContractRenewalServiceImplTest {

    @Mock private ContractRenewalRepository renewalRepository;
    @Mock private RentalContractRepository contractRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private HouseAccessService houseAccessService;
    @Mock private RentalContractService rentalContractService;
    @Spy private ContractRenewalMapper renewalMapper = new ContractRenewalMapper();

    @InjectMocks private ContractRenewalServiceImpl contractRenewalService;

    @Test
    void createRenewal_appliesRenewalToTheIntendedContractAndRecordsHistory() {
        RentalContract contract = RentalContract.builder()
                .id(7L)
                .ownerId(1L)
                .contractNo("CT-09-22-abc123def")
                .roomId(10L)
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().minusDays(5))
                .monthlyRent(new BigDecimal("100.00"))
                .securityDeposit(new BigDecimal("50.00"))
                .dueDay(5)
                .status(RentalContractStatus.EXPIRED)
                .build();
        User createdBy = User.builder().id(1L).username("owner1").build();
        LocalDate newEndDate = LocalDate.now().plusMonths(6);
        CreateRenewalRequest request = new CreateRenewalRequest(
                7L, LocalDate.now().minusDays(5), newEndDate,
                new BigDecimal("100.00"), new BigDecimal("125.00"), "renewed for 6 months", null);

        when(contractRepository.findById(7L)).thenReturn(Optional.of(contract));
        when(houseAccessService.resolveContractHouseId(7L)).thenReturn(99L);
        when(securityUtils.getCurrentUser()).thenReturn(createdBy);
        when(renewalRepository.save(any(ContractRenewal.class))).thenAnswer(inv -> {
            ContractRenewal renewal = inv.getArgument(0);
            renewal.setId(55L);
            return renewal;
        });

        ContractRenewalResponse response = contractRenewalService.createRenewal(request);

        InOrder inOrder = inOrder(houseAccessService, rentalContractService, renewalRepository);
        inOrder.verify(houseAccessService).resolveContractHouseId(7L);
        inOrder.verify(rentalContractService).applyRenewal(7L, newEndDate);
        inOrder.verify(renewalRepository).save(any(ContractRenewal.class));

        ArgumentCaptor<ContractRenewal> captor = ArgumentCaptor.forClass(ContractRenewal.class);
        verify(renewalRepository).save(captor.capture());
        ContractRenewal saved = captor.getValue();
        assertThat(saved.getRentalContract().getId()).isEqualTo(7L);
        assertThat(saved.getPreviousEndDate()).isEqualTo(LocalDate.now().minusDays(5));
        assertThat(saved.getNewEndDate()).isEqualTo(newEndDate);
        assertThat(saved.getPreviousRent()).isEqualByComparingTo("100.00");
        assertThat(saved.getNewRent()).isEqualByComparingTo("125.00");
        assertThat(saved.getNotes()).isEqualTo("renewed for 6 months");
        assertThat(saved.getCreatedBy().getId()).isEqualTo(1L);

        assertThat(response.id()).isEqualTo(55L);
        assertThat(response.contractId()).isEqualTo(7L);
        assertThat(response.newEndDate()).isEqualTo(newEndDate);
        assertThat(response.createdByName()).isEqualTo("owner1");
    }

    @Test
    void createRenewal_deniesAccessWhenContractIsNotAccessible() {
        RentalContract contract = RentalContract.builder().id(7L).roomId(10L).build();
        CreateRenewalRequest request = new CreateRenewalRequest(
                7L, null, LocalDate.now().plusMonths(6),
                new BigDecimal("100.00"), new BigDecimal("125.00"), null, null);

        when(contractRepository.findById(7L)).thenReturn(Optional.of(contract));
        when(houseAccessService.resolveContractHouseId(7L))
                .thenThrow(new AccessDeniedException("not allowed"));

        assertThatThrownBy(() -> contractRenewalService.createRenewal(request))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(rentalContractService);
        verify(renewalRepository, never()).save(any());
    }

    @Test
    void createRenewal_doesNothingWhenContractDoesNotExist() {
        CreateRenewalRequest request = new CreateRenewalRequest(
                7L, null, LocalDate.now().plusMonths(6),
                new BigDecimal("100.00"), new BigDecimal("125.00"), null, null);

        when(contractRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractRenewalService.createRenewal(request))
                .isInstanceOf(RentalContractNotFoundException.class);

        verify(rentalContractService, never()).applyRenewal(eq(7L), any());
        verify(renewalRepository, never()).save(any());
    }
}