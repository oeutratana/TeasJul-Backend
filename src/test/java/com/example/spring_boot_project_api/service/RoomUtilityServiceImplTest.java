package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.spring_boot_project_api.dto.response.RoomUtilityResponse;
import com.example.spring_boot_project_api.enums.UtilityBillingMethod;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.mapper.RoomUtilityMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomUtility;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomUtilityRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.impl.RoomUtilityServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

class RoomUtilityServiceImplTest {

    private RoomUtilityRepository roomUtilityRepository;
    private UtilityTypeRepository utilityTypeRepository;
    private UserRepository userRepository;
    private RoomUtilityServiceImpl service;
    private Room room;
    private User owner;
    private UtilityType utilityType;

    @BeforeEach
    void setUp() {
        roomUtilityRepository = org.mockito.Mockito.mock(RoomUtilityRepository.class);
        RoomRepository roomRepository = org.mockito.Mockito.mock(RoomRepository.class);
        HouseRepository houseRepository = org.mockito.Mockito.mock(HouseRepository.class);
        utilityTypeRepository = org.mockito.Mockito.mock(UtilityTypeRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        UserHouseAccessRepository accessRepository = org.mockito.Mockito.mock(UserHouseAccessRepository.class);
        SecurityUtils securityUtils = org.mockito.Mockito.mock(SecurityUtils.class);
        service = new RoomUtilityServiceImpl(roomUtilityRepository, roomRepository, houseRepository,
                utilityTypeRepository, userRepository, accessRepository, new RoomUtilityMapper(), securityUtils);

        owner = User.builder().id(1L).build();
        room = new Room();
        room.setId(10L);
        room.setHouseId(20L);
        room.setRoomNumber("A-1");
        utilityType = UtilityType.builder().id(30L).name("Internet").code("INTERNET")
                .unit("month").billingMethod(UtilityBillingMethod.FIXED)
                .status(UtilityTypeStatus.ACTIVE).build();

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(houseRepository.findById(20L)).thenReturn(Optional.of(House.builder().id(20L)
                .owner(owner).build()));
        when(utilityTypeRepository.findByIdAndOwnerId(30L, 1L)).thenReturn(Optional.of(utilityType));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void assignIncludedUtilityPersistsAndReturnsIncludedFlag() {
        when(roomUtilityRepository.findByRoomIdAndUtilityTypeId(10L, 30L)).thenReturn(Optional.empty());
        when(roomUtilityRepository.save(any(RoomUtility.class))).thenAnswer(invocation -> {
            RoomUtility utility = invocation.getArgument(0);
            utility.setId(40L);
            return utility;
        });

        RoomUtilityResponse response = service.assignUtility(10L, 30L, true);

        assertThat(response.id()).isEqualTo(40L);
        assertThat(response.isIncluded()).isTrue();
        assertThat(response.utilityTypeName()).isEqualTo("Internet");
    }

    @Test
    void reassigningInactiveUtilityUpdatesIncludedFlag() {
        RoomUtility inactive = RoomUtility.builder().id(40L).owner(owner).roomId(10L)
                .utilityTypeId(30L).status(UtilityTypeStatus.INACTIVE).isIncluded(false).build();
        when(roomUtilityRepository.findByRoomIdAndUtilityTypeId(10L, 30L))
                .thenReturn(Optional.of(inactive));
        when(roomUtilityRepository.save(inactive)).thenReturn(inactive);

        RoomUtilityResponse response = service.assignUtility(10L, 30L, true);

        assertThat(inactive.getStatus()).isEqualTo(UtilityTypeStatus.ACTIVE);
        assertThat(response.isIncluded()).isTrue();
    }
}
