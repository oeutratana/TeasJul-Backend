package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.GrantHouseAccessRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.UserHouseAccessResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseAccessNotFoundException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.HouseMapper;
import com.example.spring_boot_project_api.mapper.UserHouseAccessMapper;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.impl.UserHouseAccessServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class UserHouseAccessServiceImplTest {

    @Mock
    private UserHouseAccessRepository userHouseAccessRepository;
    @Mock
    private HouseRepository houseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private SecurityUtils securityUtils;

    @org.mockito.Spy
    private HouseMapper houseMapper = new HouseMapper();
    @org.mockito.Spy
    private UserHouseAccessMapper userHouseAccessMapper = new UserHouseAccessMapper();
    @org.mockito.Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private UserHouseAccessServiceImpl service;

    private User owner;
    private User manager;
    private House house;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("ownerA").email("ownerA@example.com")
                .passwordHash("h").firstName("Owner").lastName("A").status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        manager = User.builder().id(15L).username("managerA").email("managerA@example.com")
                .passwordHash("h").firstName("Manager").lastName("A").status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        house = House.builder().id(3L).owner(owner).name("Sunrise")
                .slug("sunrise").code("SR001").address("#1").totalRooms(10)
                .availableRooms(5).status(HouseStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private void authOwner() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
    }

    @Test
    void grantAccessAllowsOwnerAssigningManager() {
        authOwner();
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        when(userRepository.findById(15L)).thenReturn(Optional.of(manager));
        when(securityUtils.hasRole(15L, SecurityUtils.ROLE_MANAGER)).thenReturn(true);
        when(userHouseAccessRepository.existsByUserIdAndHouseId(15L, 3L)).thenReturn(false);

        var saved = new com.example.spring_boot_project_api.model.UserHouseAccess(
                new com.example.spring_boot_project_api.model.UserHouseAccessId(15L, 3L),
                manager, house, LocalDateTime.now());
        when(userHouseAccessRepository.saveAndFlush(any())).thenReturn(saved);

        UserHouseAccessResponse response =
                service.grantAccess(3L, new GrantHouseAccessRequest(15L));

        assertThat(response.userId()).isEqualTo(15L);
        assertThat(response.houseId()).isEqualTo(3L);
    }

    @Test
    void grantAccessDeniesNonManagerTarget() {
        authOwner();
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        when(userRepository.findById(15L)).thenReturn(Optional.of(manager));
        when(securityUtils.hasRole(15L, SecurityUtils.ROLE_MANAGER)).thenReturn(false);

        assertThatThrownBy(() -> service.grantAccess(3L, new GrantHouseAccessRequest(15L)))
                .isInstanceOf(AccessDeniedException.class);
        verify(userHouseAccessRepository, never()).saveAndFlush(any());
    }

    @Test
    void grantAccessDeniesNonOwner() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        assertThatThrownBy(() -> service.grantAccess(3L, new GrantHouseAccessRequest(15L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void grantAccessDeniesWhenHouseNotOwned() {
        authOwner();
        house.getOwner().setId(99L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));

        assertThatThrownBy(() -> service.grantAccess(3L, new GrantHouseAccessRequest(15L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void grantAccessThrowsWhenTargetMissing() {
        authOwner();
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        when(userRepository.findById(15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.grantAccess(3L, new GrantHouseAccessRequest(15L)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void grantAccessThrowsDuplicateWhenAlreadyExists() {
        authOwner();
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        when(userRepository.findById(15L)).thenReturn(Optional.of(manager));
        when(securityUtils.hasRole(15L, SecurityUtils.ROLE_MANAGER)).thenReturn(true);
        when(userHouseAccessRepository.existsByUserIdAndHouseId(15L, 3L)).thenReturn(true);

        assertThatThrownBy(() -> service.grantAccess(3L, new GrantHouseAccessRequest(15L)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void revokeAccessOwnerOnly() {
        authOwner();
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        when(userHouseAccessRepository.existsByUserIdAndHouseId(15L, 3L)).thenReturn(true);

        service.revokeAccess(3L, 15L);

        verify(userHouseAccessRepository).deleteByUserIdAndHouseId(15L, 3L);
    }

    @Test
    void revokeAccessThrowsNotFoundWhenMissing() {
        authOwner();
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        when(userHouseAccessRepository.existsByUserIdAndHouseId(15L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> service.revokeAccess(3L, 15L))
                .isInstanceOf(HouseAccessNotFoundException.class);
    }

    @Test
    void revokeAccessDeniesNonOwner() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        assertThatThrownBy(() -> service.revokeAccess(3L, 15L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getHouseUsersDeniesNonOwner() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        assertThatThrownBy(() -> service.getHouseUsers(3L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAccessCandidatesReturnsOnlyManagersWithoutAccess() {
        authOwner();
        house.getOwner().setId(1L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));

        User otherManager = User.builder().id(72L).username("rathana")
                .passwordHash("h").firstName("R").lastName("A").status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(userRoleRepository.findUsersByRoleCode(SecurityUtils.ROLE_MANAGER))
                .thenReturn(List.of(manager, otherManager));
        when(userHouseAccessRepository.existsByUserIdAndHouseId(15L, 3L)).thenReturn(true);
        when(userHouseAccessRepository.existsByUserIdAndHouseId(72L, 3L)).thenReturn(false);

        var candidates = service.getAccessCandidates(3L);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).id()).isEqualTo(72L);
        assertThat(candidates.get(0).username()).isEqualTo("rathana");
    }

    @Test
    void getUserHousesReturnsAssignedForManager() {
        when(securityUtils.getCurrentUserId()).thenReturn(15L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(true);
        when(userHouseAccessRepository.findHousesByUserId(15L)).thenReturn(List.of(house));

        List<HouseResponse> houses = service.getUserHouses();

        assertThat(houses).hasSize(1);
        assertThat(houses.get(0).id()).isEqualTo(3L);
    }

    @Test
    void getUserHousesReturnsOwnedForOwner() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(houseRepository.findAllByOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(house));

        List<HouseResponse> houses = service.getUserHouses();

        assertThat(houses).hasSize(1);
        assertThat(houses.get(0).id()).isEqualTo(3L);
    }

    @Test
    void getUserHousesDeniesTenant() {
        when(securityUtils.getCurrentUserId()).thenReturn(15L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        assertThatThrownBy(() -> service.getUserHouses())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getOwnedHouseThrowsNotFound() {
        authOwner();
        when(houseRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.grantAccess(3L, new GrantHouseAccessRequest(15L)))
                .isInstanceOf(HouseNotFoundException.class);
    }
}
