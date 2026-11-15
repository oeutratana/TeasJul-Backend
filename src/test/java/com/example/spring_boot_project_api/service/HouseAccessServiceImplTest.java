package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.impl.HouseAccessServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class HouseAccessServiceImplTest {

    private static final long HOUSE_ID = 10L;

    @Mock
    private HouseRepository houseRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RentalContractRepository contractRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserHouseAccessRepository userHouseAccessRepository;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private HouseAccessServiceImpl houseAccessService;

    private House houseOwnedByRecord() {
        House house = new House();
        house.setId(HOUSE_ID);
        house.setOwner(null);
        house.setOwnerRecordId(99L);
        return house;
    }

    @Test
    void canAccessHouse_houseOwnedByNonLoginRecord_superAdminAllowedWithoutNpe() {
        when(houseRepository.findById(HOUSE_ID)).thenReturn(Optional.of(houseOwnedByRecord()));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);

        assertThat(houseAccessService.canAccessHouse(HOUSE_ID)).isTrue();
    }

    @Test
    void canAccessHouse_houseOwnedByNonLoginRecord_tenantDeniedWithoutNpe() {
        when(houseRepository.findById(HOUSE_ID)).thenReturn(Optional.of(houseOwnedByRecord()));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThat(houseAccessService.canAccessHouse(HOUSE_ID)).isFalse();
    }

    @Test
    void requireOwnerHouseAccess_houseOwnedByNonLoginRecord_ownerDeniedWithoutNpe() {
        when(houseRepository.findById(HOUSE_ID)).thenReturn(Optional.of(houseOwnedByRecord()));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        assertThatThrownBy(() -> houseAccessService.requireOwnerHouseAccess(HOUSE_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireOwnerHouseAccess_ownerOwned_ownerAllowed() {
        House house = houseOwnedByRecord();
        User owner = new User();
        owner.setId(7L);
        house.setOwner(owner);
        when(houseRepository.findById(HOUSE_ID)).thenReturn(Optional.of(house));
        when(securityUtils.getCurrentUserId()).thenReturn(7L);

        houseAccessService.requireOwnerHouseAccess(HOUSE_ID);
    }

    @Test
    void getAccessibleHouseIds_nonOwnerManagerReturnsEmpty() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);

        assertThat(houseAccessService.getAccessibleHouseIds()).isEmpty();
    }
}