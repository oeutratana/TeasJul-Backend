package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.spring_boot_project_api.dto.request.MeterImportRequest;
import com.example.spring_boot_project_api.dto.request.MeterImportRow;
import com.example.spring_boot_project_api.dto.response.MeterImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.MeterImportPreviewRowResponse;
import com.example.spring_boot_project_api.dto.response.MeterImportResult;
import com.example.spring_boot_project_api.dto.response.UtilityMeterResponse;
import com.example.spring_boot_project_api.enums.UtilityBillingMethod;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseStatus;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.impl.MeterImportServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MeterImportServiceImplTest {

    @Mock
    private UtilityMeterService utilityMeterService;
    @Mock
    private HouseRepository houseRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private UtilityTypeRepository utilityTypeRepository;
    @Mock
    private UtilityMeterRepository meterRepository;
    @Mock
    private UserHouseAccessRepository userHouseAccessRepository;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private MeterImportServiceImpl service;

    private User owner;
    private House house;
    private Room room;
    private UtilityType type;

    @BeforeEach
    void setUp() {
        owner = User.builder()
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
                .address("#123, Street 271")
                .status(HouseStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 8, 23, 22, 55, 25))
                .updatedAt(LocalDateTime.of(2026, 8, 23, 22, 55, 25))
                .build();

        room = new Room();
        room.setId(10L);
        room.setOwnerId(15L);
        room.setHouseId(1L);
        room.setRoomNumber("A101");

        type = UtilityType.builder()
                .id(5L)
                .owner(owner)
                .name("Electricity")
                .code("ELEC")
                .unit("kWh")
                .billingMethod(UtilityBillingMethod.METERED)
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(15L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
        when(houseRepository.findByIdAndOwnerId(1L, 15L)).thenReturn(Optional.of(house));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(utilityTypeRepository.findById(5L)).thenReturn(Optional.of(type));
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(any(), any(), any()))
                .thenReturn(false);
        when(meterRepository.findByRoomIdAndUtilityTypeId(10L, 5L)).thenReturn(List.of());
    }

    private static final String CSV_HEADER =
            "HouseId,RoomId,UtilityTypeId,MeterNumber,InstalledAt,Status\n";

    private String csv(String... rows) {
        return CSV_HEADER + String.join("\n", rows);
    }

    @Test
    void previewParsesValidCsv() {
        MeterImportPreviewResponse preview = service.previewImport("meters.csv",
                csv("1,10,5,EL-001,2026-01-15,ACTIVE").getBytes());

        assertThat(preview.totalRows()).isEqualTo(1);
        assertThat(preview.invalidRows()).isEqualTo(0);
        assertThat(preview.rows()).hasSize(1);
        MeterImportPreviewRowResponse row = preview.rows().get(0);
        assertThat(row.valid()).isTrue();
        assertThat(row.meterNumber()).isEqualTo("EL-001");
        assertThat(row.houseId()).isEqualTo("1");
        assertThat(row.roomId()).isEqualTo("10");
    }

    @Test
    void previewFlagsInvalidAndDuplicateRows() {
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(15L, 5L, "EL-001"))
                .thenReturn(true);

        MeterImportPreviewResponse preview = service.previewImport("meters.csv", csv(
                "1,10,5,EL-001,2026-01-15,ACTIVE",
                ",,,,2026-13-01,BOGUS").getBytes());

        assertThat(preview.totalRows()).isEqualTo(2);
        assertThat(preview.invalidRows()).isEqualTo(2);
        assertThat(preview.rows().get(0).errors()).anyMatch(e -> e.contains("already exists"));
        assertThat(preview.rows().get(1).errors()).anyMatch(e -> e.contains("HouseId is required"));
        assertThat(preview.rows().get(1).errors()).anyMatch(e -> e.contains("UtilityTypeId is required"));
        assertThat(preview.rows().get(1).errors()).anyMatch(e -> e.contains("MeterNumber is required"));
        assertThat(preview.rows().get(1).errors()).anyMatch(e -> e.contains("InstalledAt is invalid"));
        assertThat(preview.rows().get(1).errors()).anyMatch(e -> e.contains("Invalid status"));
    }

    @Test
    void previewRejectsFixedBillingUtilityType() {
        type.setBillingMethod(UtilityBillingMethod.FIXED);

        MeterImportPreviewResponse preview = service.previewImport("meters.csv",
                csv("1,10,5,EL-001,2026-01-15,ACTIVE").getBytes());

        assertThat(preview.invalidRows()).isEqualTo(1);
        assertThat(preview.rows().get(0).errors()).anyMatch(e -> e.contains("METERED"));
    }

    @Test
    void importCreatesMetersForValidRows() {
        UtilityMeterResponse saved = new UtilityMeterResponse(
                1L, 15L, 1L, 10L, 5L, "EL-001", null, UtilityMeterStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(utilityMeterService.createMeter(any())).thenReturn(saved);

        MeterImportRequest request = new MeterImportRequest(List.of(
                MeterImportRow.of("1", "10", "5", "EL-001", "2026-01-15", "ACTIVE")));
        MeterImportResult result = service.importMeters(request);

        assertThat(result.metersCreated()).isEqualTo(1);
        verify(utilityMeterService, times(1)).createMeter(any());
        assertThat(result.messages()).hasSize(1);
    }

    @Test
    void previewRejectsEmptyContent() {
        assertThatThrownBy(() -> service.previewImport("meters.csv", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void importRejectsEmptyRows() {
        assertThatThrownBy(() -> service.importMeters(new MeterImportRequest(List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void previewParsesCsvWithoutHouseIdColumnWhenHouseIdSelected() {
        String csvContent = "RoomId,UtilityTypeId,MeterNumber,InstalledAt,Status\n" +
                "10,5,W-001,2026-01-15,\n";

        MeterImportPreviewResponse preview = service.previewImport(1L, "meters.csv", csvContent.getBytes());

        assertThat(preview.totalRows()).isEqualTo(1);
        assertThat(preview.invalidRows()).isEqualTo(0);
        MeterImportPreviewRowResponse row = preview.rows().get(0);
        assertThat(row.valid()).isTrue();
        assertThat(row.houseId()).isEqualTo("1");
        assertThat(row.meterNumber()).isEqualTo("W-001");
    }

    @Test
    void importWithSelectedHouseIdOverridesRowHouseId() {
        UtilityMeterResponse saved = new UtilityMeterResponse(
                1L, 15L, 1L, 10L, 5L, "W-001", null, UtilityMeterStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(utilityMeterService.createMeter(any())).thenReturn(saved);

        MeterImportRequest request = new MeterImportRequest(1L, List.of(
                MeterImportRow.of(null, "10", "5", "W-001", null, null)));
        MeterImportResult result = service.importMeters(request);

        assertThat(result.metersCreated()).isEqualTo(1);
        verify(utilityMeterService, times(1)).createMeter(any());
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
                .address("#456, Street 271")
                .status(HouseStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 8, 23, 22, 55, 25))
                .updatedAt(LocalDateTime.of(2026, 8, 23, 22, 55, 25))
                .build();
        Room roomB = new Room();
        roomB.setId(20L);
        roomB.setOwnerId(16L);
        roomB.setHouseId(2L);
        roomB.setRoomNumber("B101");

        when(securityUtils.getCurrentUserId()).thenReturn(16L);
        when(houseRepository.findByIdAndOwnerId(2L, 16L)).thenReturn(Optional.of(houseB));
        when(roomRepository.findById(20L)).thenReturn(Optional.of(roomB));
        UtilityType typeB = UtilityType.builder()
                .id(5L)
                .owner(otherOwner)
                .name("Electricity")
                .code("ELEC")
                .unit("kWh")
                .billingMethod(UtilityBillingMethod.METERED)
                .build();
        when(utilityTypeRepository.findById(5L)).thenReturn(Optional.of(typeB));
        // Owner A already holds EL-001 for Electricity; Owner B does not.
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(15L, 5L, "EL-001"))
                .thenReturn(true);
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(16L, 5L, "EL-001"))
                .thenReturn(false);

        MeterImportPreviewResponse preview = service.previewImport("meters.csv",
                csv("2,20,5,EL-001,2026-01-15,ACTIVE").getBytes());

        assertThat(preview.invalidRows()).isZero();
        assertThat(preview.rows().get(0).valid()).isTrue();
    }

    @Test
    void sameMeterNumberAllowedForDifferentUtilityTypeOfSameOwner() {
        UtilityType water = UtilityType.builder()
                .id(6L)
                .owner(owner)
                .name("Water")
                .code("WATER")
                .unit("m³")
                .billingMethod(UtilityBillingMethod.METERED)
                .build();
        when(utilityTypeRepository.findById(6L)).thenReturn(Optional.of(water));
        // Electricity already uses EL-001; a Water meter with the same number is a
        // different (owner, type, number) scope and is allowed.
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(15L, 5L, "EL-001"))
                .thenReturn(true);
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(15L, 6L, "EL-001"))
                .thenReturn(false);

        MeterImportPreviewResponse preview = service.previewImport("meters.csv",
                csv("1,10,6,EL-001,2026-01-15,ACTIVE").getBytes());

        assertThat(preview.invalidRows()).isZero();
        assertThat(preview.rows().get(0).valid()).isTrue();
    }

    @Test
    void duplicateMeterNumberForSameOwnerUtilityTypeInFileIsRejected() {
        when(meterRepository.existsByOwnerIdAndUtilityTypeIdAndMeterNumber(15L, 5L, "EL-001"))
                .thenReturn(false);

        MeterImportPreviewResponse preview = service.previewImport("meters.csv",
                csv("1,10,5,EL-001,2026-01-15,ACTIVE",
                        "1,,5,EL-001,2026-01-15,ACTIVE").getBytes());

        assertThat(preview.invalidRows()).isEqualTo(1);
        assertThat(preview.rows().get(1).errors())
                .anyMatch(e -> e.contains("Duplicate meter number in file"));
    }

    @Test
    void managerCannotImportMeterForHouseWithoutAccess() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(true);
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house));
        when(userHouseAccessRepository.existsByUserIdAndHouseId(15L, 1L)).thenReturn(false);

        MeterImportPreviewResponse preview = service.previewImport("meters.csv",
                csv("1,10,5,EL-001,2026-01-15,ACTIVE").getBytes());

        assertThat(preview.invalidRows()).isEqualTo(1);
        assertThat(preview.rows().get(0).errors())
                .anyMatch(e -> e.contains("do not have access"));
    }

    @Test
    void managerCanImportMeterForHouseWithAccess() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(true);
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house));
        when(userHouseAccessRepository.existsByUserIdAndHouseId(15L, 1L)).thenReturn(true);

        MeterImportPreviewResponse preview = service.previewImport("meters.csv",
                csv("1,10,5,EL-001,2026-01-15,ACTIVE").getBytes());

        assertThat(preview.invalidRows()).isZero();
        assertThat(preview.rows().get(0).valid()).isTrue();
    }
}