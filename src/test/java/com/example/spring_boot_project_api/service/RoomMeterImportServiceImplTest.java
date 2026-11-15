package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

import com.example.spring_boot_project_api.dto.request.CreateUtilityMeterRequest;
import com.example.spring_boot_project_api.dto.request.RoomMeterImportRequest;
import com.example.spring_boot_project_api.dto.request.RoomMeterImportRow;
import com.example.spring_boot_project_api.dto.response.RoomMeterImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.RoomMeterImportResult;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.enums.UtilityBillingMethod;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.exception.RoomMeterImportValidationException;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseStatus;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.RoomUtility;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.RoomUtilityRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.impl.RoomMeterImportServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomMeterImportServiceImplTest {

    @Mock
    private RoomService roomService;
    @Mock
    private HouseRepository houseRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private UtilityTypeRepository utilityTypeRepository;
    @Mock
    private UtilityMeterRepository meterRepository;
    @Mock
    private RoomUtilityRepository roomUtilityRepository;
    @Mock
    private UtilityMeterService utilityMeterService;
    @Mock
    private UserHouseAccessRepository userHouseAccessRepository;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private RoomMeterImportServiceImpl service;

    private House house;
    private RoomType roomType;
    private UtilityType electricity;
    private UtilityType water;
    private RoomResponse savedRoom;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .id(15L)
                .username("ownerA")
                .email("ownerA@example.com")
                .passwordHash("h")
                .firstName("Owner")
                .lastName("A")
                .phone("+855")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();

        house = House.builder()
                .id(1L)
                .owner(owner)
                .name("Sunrise Residence")
                .slug("sunrise-residence")
                .code("SR001")
                .status(HouseStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();

        roomType = new RoomType();
        roomType.setId(2L);
        roomType.setOwnerId(15L);
        roomType.setName("Standard");
        roomType.setCode("STD");

        electricity = meteredUtility(5L, "Electricity", "ELEC");
        water = meteredUtility(6L, "Water", "WATER");

        savedRoom = new RoomResponse();
        savedRoom.setId(10L);
        savedRoom.setRoomNumber("A101");

        when(securityUtils.getCurrentUserId()).thenReturn(15L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
        when(houseRepository.findByIdAndOwnerId(1L, 15L)).thenReturn(Optional.of(house));
        when(roomTypeRepository.findById(2L)).thenReturn(Optional.of(roomType));
        when(roomRepository.findByHouseId(1L)).thenReturn(List.of());
        when(utilityTypeRepository.findByOwnerIdIn(List.of(15L)))
                .thenReturn(List.of(electricity, water));
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(any(), any(), anyString()))
                .thenReturn(false);
        when(roomService.createRoom(any())).thenReturn(savedRoom);
    }

    private UtilityType meteredUtility(Long id, String name, String code) {
        return UtilityType.builder()
                .id(id)
                .name(name)
                .code(code)
                .unit("kWh")
                .billingMethod(UtilityBillingMethod.METERED)
                .build();
    }

    private RoomMeterImportRow row(String roomNumber, String electricityMeter,
            String electricityStatus, String waterMeter, String waterStatus) {
        return RoomMeterImportRow.of(roomNumber, "Room " + roomNumber, "1", "150", "150",
                "AVAILABLE", "", electricityMeter, electricityStatus, waterMeter, waterStatus);
    }

    private RoomMeterImportRequest request(RoomMeterImportRow... rows) {
        return new RoomMeterImportRequest(1L, 2L, LocalDate.of(2026, 9, 22), List.of(rows));
    }

    private RoomMeterImportRequest request(Long houseId, RoomMeterImportRow... rows) {
        return new RoomMeterImportRequest(houseId, 2L, LocalDate.of(2026, 9, 22), List.of(rows));
    }

    private static final String CSV_HEADER =
            "Room Number,Room Name,Floor,Monthly Rent,Deposit,Status,Description,"
                    + "Electricity Meter,Electricity Status,Water Meter,Water Status\n";

    private String csv(String... rows) {
        return CSV_HEADER + String.join("\n", rows);
    }

    @Test
    void importCreatesRoomAndElectricityAndWaterMeters() {
        RoomMeterImportResult result = service.importRoomsWithMeters(request(
                row("A101", "EL-001", "ACTIVE", "W-001", "ACTIVE")));

        assertThat(result.roomsCreated()).isEqualTo(1);
        assertThat(result.metersCreated()).isEqualTo(2);
        ArgumentCaptor<CreateUtilityMeterRequest> captor = ArgumentCaptor.forClass(CreateUtilityMeterRequest.class);
        verify(utilityMeterService, times(2)).createMeter(captor.capture());
        assertThat(captor.getAllValues()).extracting(CreateUtilityMeterRequest::utilityTypeId)
                .containsExactlyInAnyOrder(5L, 6L);
        verify(roomService).createRoom(any());
    }

    @Test
    void importCreatesElectricityOnly() {
        RoomMeterImportResult result = service.importRoomsWithMeters(request(
                row("A101", "EL-001", "ACTIVE", null, null)));

        assertThat(result.metersCreated()).isEqualTo(1);
        verify(utilityMeterService).createMeter(any());
    }

    @Test
    void importCreatesWaterOnly() {
        RoomMeterImportResult result = service.importRoomsWithMeters(request(
                row("A101", null, null, "W-001", "ACTIVE")));

        assertThat(result.metersCreated()).isEqualTo(1);
        verify(utilityMeterService).createMeter(any());
    }

    @Test
    void emptyElectricityFieldCreatesNoElectricityMeter() {
        RoomMeterImportResult result = service.importRoomsWithMeters(request(
                row("A101", null, null, "W-001", "ACTIVE")));

        assertThat(result.metersCreated()).isEqualTo(1);
        ArgumentCaptor<CreateUtilityMeterRequest> captor = ArgumentCaptor.forClass(CreateUtilityMeterRequest.class);
        verify(utilityMeterService).createMeter(captor.capture());
        assertThat(captor.getValue().utilityTypeId()).isEqualTo(6L);
    }

    @Test
    void emptyWaterFieldCreatesNoWaterMeter() {
        RoomMeterImportResult result = service.importRoomsWithMeters(request(
                row("A101", "EL-001", "ACTIVE", null, null)));

        assertThat(result.metersCreated()).isEqualTo(1);
        ArgumentCaptor<CreateUtilityMeterRequest> captor = ArgumentCaptor.forClass(CreateUtilityMeterRequest.class);
        verify(utilityMeterService).createMeter(captor.capture());
        assertThat(captor.getValue().utilityTypeId()).isEqualTo(5L);
    }

    @Test
    void garbageColumnsAreIgnoredAndNeverCreateGarbageMeters() {
        String content = csv("A101,Room A101,1,150,150,AVAILABLE,,EL-001,ACTIVE,W-001,ACTIVE,G-001,GARBAGE");

        RoomMeterImportPreviewResponse preview = service.previewImport(1L, 2L, null,
                "rooms.csv", content.getBytes());
        RoomMeterImportResult result = service.importRoomsWithMeters(request(
                row("A101", "EL-001", "ACTIVE", "W-001", "ACTIVE")));

        assertThat(preview.invalidRows()).isZero();
        assertThat(preview.rows().get(0).metersToCreate()).isEqualTo(2);
        assertThat(result.metersCreated()).isEqualTo(2);
        verify(utilityMeterService, times(2)).createMeter(any());
    }

    @Test
    void ownerIsolationRejectsHouseOutsideCurrentOwner() {
        when(houseRepository.findByIdAndOwnerId(1L, 15L)).thenReturn(Optional.empty());

        RoomMeterImportPreviewResponse preview = service.previewImport(1L, 2L, null,
                "rooms.csv", csv("A101,Room A101,1,150,150,AVAILABLE,,EL-001,ACTIVE,,,").getBytes());

        assertThat(preview.invalidRows()).isEqualTo(1);
        assertThat(preview.rows().get(0).errors())
                .anyMatch(error -> error.contains("do not have access"));
    }

    @Test
    void invalidRoomTypeRejectsPreview() {
        when(roomTypeRepository.findById(2L)).thenReturn(Optional.empty());

        RoomMeterImportPreviewResponse preview = service.previewImport(1L, 2L, null,
                "rooms.csv", csv("A101,Room A101,1,150,150,AVAILABLE,,EL-001,ACTIVE,,,").getBytes());

        assertThat(preview.invalidRows()).isEqualTo(1);
        assertThat(preview.rows().get(0).errors())
                .containsExactly("Room Type not found or invalid for this House.");
    }

    @Test
    void duplicateRoomValidationPreventsImport() {
        Room existing = new Room();
        existing.setRoomNumber("A101");
        when(roomRepository.findByHouseId(1L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.importRoomsWithMeters(request(
                row("A101", "EL-001", "ACTIVE", null, null))))
                .isInstanceOf(RoomMeterImportValidationException.class);
        verify(roomService, never()).createRoom(any());
        verify(utilityMeterService, never()).createMeter(any());
    }

    @Test
    void previewAndCommitUseTheSameElectricityWaterCsvStructure() {
        String content = csv("A101,Room A101,1,150,150,AVAILABLE,,EL-001,ACTIVE,W-001,ACTIVE");

        RoomMeterImportPreviewResponse preview = service.previewImport(1L, 2L, null,
                "rooms.csv", content.getBytes());
        RoomMeterImportResult result = service.importRoomsWithMeters(request(
                row("A101", "EL-001", "ACTIVE", "W-001", "ACTIVE")));

        assertThat(preview.invalidRows()).isZero();
        assertThat(preview.rows().get(0).metersToCreate()).isEqualTo(result.metersCreated());
        assertThat(preview.rows().get(0).electricityMeter()).isEqualTo("EL-001");
        assertThat(preview.rows().get(0).waterMeter()).isEqualTo("W-001");
    }

    @Test
    void invalidRowPreventsAnyCombinedImportWrites() {
        RoomMeterImportRow valid = row("A101", "EL-001", "ACTIVE", "W-001", "ACTIVE");
        RoomMeterImportRow invalid = RoomMeterImportRow.of("A102", "Room B", "1",
                "not-money", "150", "AVAILABLE", "", null, null, null, null);

        assertThatThrownBy(() -> service.importRoomsWithMeters(request(valid, invalid)))
                .isInstanceOf(RoomMeterImportValidationException.class);
        verify(roomService, never()).createRoom(any());
        verify(utilityMeterService, never()).createMeter(any());
    }

    @Test
    void suppliedInstallDateIsUsedForMeters() {
        service.importRoomsWithMeters(new RoomMeterImportRequest(1L, 2L,
                LocalDate.of(2026, 9, 22), List.of(row("A101", "EL-001", "ACTIVE", null, null))));

        ArgumentCaptor<CreateUtilityMeterRequest> captor = ArgumentCaptor.forClass(CreateUtilityMeterRequest.class);
        verify(utilityMeterService).createMeter(captor.capture());
        assertThat(captor.getValue().installedAt()).isEqualTo(LocalDate.of(2026, 9, 22));
    }

    @Test
    void emptyInstallDateUsesCurrentDate() {
        LocalDate before = LocalDate.now();
        service.importRoomsWithMeters(new RoomMeterImportRequest(1L, 2L, null,
                List.of(row("A101", "EL-001", "ACTIVE", null, null))));
        LocalDate after = LocalDate.now();

        ArgumentCaptor<CreateUtilityMeterRequest> captor = ArgumentCaptor.forClass(CreateUtilityMeterRequest.class);
        verify(utilityMeterService).createMeter(captor.capture());
        assertThat(captor.getValue().installedAt()).isBetween(before, after);
    }

    @Test
    void duplicateMeterValidationPreventsImport() {
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(15L, 5L, "EL-001"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.importRoomsWithMeters(request(
                row("A101", "EL-001", "ACTIVE", null, null))))
                .isInstanceOf(RoomMeterImportValidationException.class);
        verify(roomService, never()).createRoom(any());
        verify(utilityMeterService, never()).createMeter(any());
    }

    @Test
    void importAttachesElectricityAndWaterRoomUtilities() {
        when(roomUtilityRepository.findByRoomIdAndUtilityTypeId(10L, 5L)).thenReturn(Optional.empty());
        when(roomUtilityRepository.findByRoomIdAndUtilityTypeId(10L, 6L)).thenReturn(Optional.empty());

        RoomMeterImportResult result = service.importRoomsWithMeters(request(
                row("A101", "EL-001", "ACTIVE", "W-001", "ACTIVE")));

        assertThat(result.metersCreated()).isEqualTo(2);
        ArgumentCaptor<RoomUtility> captor = ArgumentCaptor.forClass(RoomUtility.class);
        verify(roomUtilityRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(RoomUtility::getUtilityTypeId)
                .containsExactlyInAnyOrder(5L, 6L);
        assertThat(captor.getAllValues())
                .allSatisfy(ru -> assertThat(ru.getStatus()).isEqualTo(UtilityTypeStatus.ACTIVE));
        assertThat(captor.getAllValues())
                .allSatisfy(ru -> assertThat(ru.getOwner().getId()).isEqualTo(15L));
    }

    @Test
    void importAttachesElectricityRoomUtilityOnlyWhenWaterIsBlank() {
        when(roomUtilityRepository.findByRoomIdAndUtilityTypeId(10L, 5L)).thenReturn(Optional.empty());

        RoomMeterImportResult result = service.importRoomsWithMeters(request(
                row("A101", "EL-001", "ACTIVE", null, null)));

        assertThat(result.metersCreated()).isEqualTo(1);
        verify(roomUtilityRepository).save(argThat(ru -> ru.getUtilityTypeId().equals(5L)));
        verify(roomUtilityRepository, never()).save(argThat(ru -> ru.getUtilityTypeId().equals(6L)));
    }

    @Test
    void importReusesExistingRoomUtilityAssignmentWithoutDuplicating() {
        RoomUtility existing = RoomUtility.builder().id(99L).roomId(10L).utilityTypeId(5L)
                .status(UtilityTypeStatus.ACTIVE).isIncluded(true).build();
        when(roomUtilityRepository.findByRoomIdAndUtilityTypeId(10L, 5L))
                .thenReturn(Optional.of(existing));

        service.importRoomsWithMeters(request(
                row("A101", "EL-001", "ACTIVE", null, null)));

        verify(roomUtilityRepository, never()).save(any(RoomUtility.class));
    }

    @Test
    void importCreatesNoRoomUtilityWhenMeterTypeMissingForOwner() {
        when(utilityTypeRepository.findByOwnerIdIn(List.of(15L)))
                .thenReturn(List.of(water));

        RoomMeterImportResult result = service.importRoomsWithMeters(request(
                row("A101", "EL-001", "ACTIVE", "W-001", "ACTIVE")));

        assertThat(result.metersCreated()).isEqualTo(1);
        verify(roomUtilityRepository).save(argThat(ru -> ru.getUtilityTypeId().equals(6L)));
    }

    @Test
    void sameMeterNumberAllowedForAnotherOwner() {
        User otherOwner = User.builder()
                .id(16L)
                .username("ownerB")
                .email("ownerB@example.com")
                .passwordHash("h")
                .firstName("Owner")
                .lastName("B")
                .phone("+855")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
        House houseB = House.builder()
                .id(2L)
                .owner(otherOwner)
                .name("Owner B Residence")
                .slug("owner-b-residence")
                .code("OBR01")
                .status(HouseStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
        when(securityUtils.getCurrentUserId()).thenReturn(16L);
        when(houseRepository.findByIdAndOwnerId(2L, 16L)).thenReturn(Optional.of(houseB));
        when(roomRepository.findByHouseId(2L)).thenReturn(List.of());
        when(utilityTypeRepository.findByOwnerIdIn(List.of(16L)))
                .thenReturn(List.of(electricity, water));
        RoomType roomTypeB = new RoomType();
        roomTypeB.setId(2L);
        roomTypeB.setOwnerId(16L);
        roomTypeB.setName("Standard");
        roomTypeB.setCode("STD");
        when(roomTypeRepository.findById(2L)).thenReturn(Optional.of(roomTypeB));
        // Owner A already holds EL-001 for Electricity; Owner B does not.
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(15L, 5L, "EL-001"))
                .thenReturn(true);
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(16L, 5L, "EL-001"))
                .thenReturn(false);

        RoomMeterImportResult result = service.importRoomsWithMeters(request(2L,
                row("B101", "EL-001", "ACTIVE", null, null)));

        assertThat(result.metersCreated()).isEqualTo(1);
        verify(utilityMeterService).createMeter(argThat(r -> r.utilityTypeId().equals(5L)));
    }

    @Test
    void sameMeterNumberAllowedForDifferentUtilityTypeOfSameOwner() {
        // Electricity already uses EDC-A01 for this owner; a Water meter with the
        // same number is a different (owner, type, number) scope, so it is allowed.
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(15L, 5L, "EDC-A01"))
                .thenReturn(true);
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(15L, 6L, "EDC-A01"))
                .thenReturn(false);

        RoomMeterImportResult result = service.importRoomsWithMeters(request(
                row("A101", null, null, "EDC-A01", "ACTIVE")));

        assertThat(result.metersCreated()).isEqualTo(1);
        verify(utilityMeterService).createMeter(argThat(r -> r.utilityTypeId().equals(6L)));
    }

    @Test
    void sameWaterMeterNumberAllowedForAnotherOwner() {
        User otherOwner = User.builder()
                .id(16L)
                .username("ownerB")
                .email("ownerB@example.com")
                .passwordHash("h")
                .firstName("Owner")
                .lastName("B")
                .phone("+855")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
        House houseB = House.builder()
                .id(2L)
                .owner(otherOwner)
                .name("Owner B Residence")
                .slug("owner-b-residence")
                .code("OBR01")
                .status(HouseStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
        when(securityUtils.getCurrentUserId()).thenReturn(16L);
        when(houseRepository.findByIdAndOwnerId(2L, 16L)).thenReturn(Optional.of(houseB));
        when(roomRepository.findByHouseId(2L)).thenReturn(List.of());
        when(utilityTypeRepository.findByOwnerIdIn(List.of(16L)))
                .thenReturn(List.of(electricity, water));
        RoomType roomTypeB = new RoomType();
        roomTypeB.setId(2L);
        roomTypeB.setOwnerId(16L);
        roomTypeB.setName("Standard");
        roomTypeB.setCode("STD");
        when(roomTypeRepository.findById(2L)).thenReturn(Optional.of(roomTypeB));
        // Owner A already holds WT-A01 for Water; Owner B does not.
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(15L, 6L, "WT-A01"))
                .thenReturn(true);
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(16L, 6L, "WT-A01"))
                .thenReturn(false);

        RoomMeterImportResult result = service.importRoomsWithMeters(request(2L,
                row("B101", null, null, "WT-A01", "ACTIVE")));

        assertThat(result.metersCreated()).isEqualTo(1);
        verify(utilityMeterService).createMeter(argThat(r -> r.utilityTypeId().equals(6L)));
    }

    @Test
    void duplicateMeterNumberForSameOwnerUtilityTypeInFileIsRejected() {
        assertThatThrownBy(() -> service.importRoomsWithMeters(request(
                row("A101", "EL-001", "ACTIVE", null, null),
                row("A102", "EL-001", "ACTIVE", null, null))))
                .isInstanceOf(RoomMeterImportValidationException.class);
        verify(roomService, never()).createRoom(any());
        verify(utilityMeterService, never()).createMeter(any());
    }
}