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

import com.example.spring_boot_project_api.dto.request.RoomImportRequest;
import com.example.spring_boot_project_api.dto.request.RoomImportRow;
import com.example.spring_boot_project_api.dto.response.RoomImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.RoomImportPreviewRowResponse;
import com.example.spring_boot_project_api.dto.response.RoomImportResult;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseStatus;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.impl.RoomImportServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomImportServiceImplTest {

    @Mock
    private RoomService roomService;
    @Mock
    private HouseRepository houseRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private UserHouseAccessRepository userHouseAccessRepository;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private RoomImportServiceImpl service;

    private User owner;
    private House house;

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

        when(securityUtils.getCurrentUserId()).thenReturn(15L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
        when(houseRepository.findByIdAndOwnerId(1L, 15L)).thenReturn(Optional.of(house));
        when(roomRepository.findByHouseId(1L)).thenReturn(List.of());

        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setOwnerId(15L);
        roomType.setName("Standard");
        roomType.setCode("STD");
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));
    }

    private static final String CSV_HEADER =
            "HouseId,RoomTypeId,RoomNumber,Name,FloorNumber,MonthlyRent,SecurityDeposit,Capacity,Status,Description\n";

    private String csv(String... rows) {
        return CSV_HEADER + String.join("\n", rows);
    }

    @Test
    void previewParsesValidCsv() {
        RoomImportPreviewResponse preview = service.previewImport("rooms.csv",
                csv("1,1,A101,Bedroom A,1,150,150,2,AVAILABLE,").getBytes());

        assertThat(preview.totalRows()).isEqualTo(1);
        assertThat(preview.invalidRows()).isEqualTo(0);
        assertThat(preview.rows()).hasSize(1);
        RoomImportPreviewRowResponse row = preview.rows().get(0);
        assertThat(row.valid()).isTrue();
        assertThat(row.roomNumber()).isEqualTo("A101");
        assertThat(row.monthlyRent()).isEqualTo("150");
    }

    @Test
    void previewFlagsInvalidAndDuplicateRows() {
        var existing = new com.example.spring_boot_project_api.model.Room();
        existing.setRoomNumber("A101");
        when(roomRepository.findByHouseId(1L)).thenReturn(List.of(existing));

        RoomImportPreviewResponse preview = service.previewImport("rooms.csv", csv(
                "1,1,A101,Bedroom A,1,150,150,2,AVAILABLE,",
                ",2,,B,1,abc,150,0,AVAILABLE,").getBytes());

        assertThat(preview.totalRows()).isEqualTo(2);
        assertThat(preview.invalidRows()).isEqualTo(2);
        assertThat(preview.rows().get(0).errors()).anyMatch(e -> e.contains("already exists"));
        assertThat(preview.rows().get(1).errors()).anyMatch(e -> e.contains("HouseId is required"));
        assertThat(preview.rows().get(1).errors()).anyMatch(e -> e.contains("RoomNumber is required"));
        assertThat(preview.rows().get(1).errors()).anyMatch(e -> e.contains("MonthlyRent is invalid"));
    }

    @Test
    void importCreatesRoomsForValidRows() {
        RoomResponse saved = new RoomResponse();
        saved.setId(1L);
        saved.setRoomNumber("A101");
        when(roomService.createRoom(any())).thenReturn(saved);

        RoomImportRequest request = new RoomImportRequest(List.of(
                RoomImportRow.of("1", "1", "A101", "Bedroom A", "1", "150", "150", "2", "AVAILABLE", null)));
        RoomImportResult result = service.importRooms(request);

        assertThat(result.roomsCreated()).isEqualTo(1);
        verify(roomService, times(1)).createRoom(any());
        assertThat(result.messages()).hasSize(1);
    }

    @Test
    void previewRejectsEmptyContent() {
        assertThatThrownBy(() -> service.previewImport("rooms.csv", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void importRejectsEmptyRows() {
        assertThatThrownBy(() -> service.importRooms(new RoomImportRequest(List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void previewParsesCsvWithoutHouseIdColumnWhenHouseIdSelected() {
        String csvContent = "RoomTypeId,RoomNumber,Name,FloorNumber,MonthlyRent,SecurityDeposit,Capacity,Status,Description\n" +
                "1,A101,Bedroom A,1,150,150,2,AVAILABLE,\n";

        RoomImportPreviewResponse preview = service.previewImport(1L, "rooms.csv", csvContent.getBytes());

        assertThat(preview.totalRows()).isEqualTo(1);
        assertThat(preview.invalidRows()).isEqualTo(0);
        RoomImportPreviewRowResponse row = preview.rows().get(0);
        assertThat(row.valid()).isTrue();
        assertThat(row.houseId()).isEqualTo("1");
        assertThat(row.roomNumber()).isEqualTo("A101");
        assertThat(row.monthlyRent()).isEqualTo("150");
    }

    @Test
    void importWithSelectedHouseIdOverridesRowHouseId() {
        RoomResponse saved = new RoomResponse();
        saved.setId(1L);
        saved.setRoomNumber("A101");
        when(roomService.createRoom(any())).thenReturn(saved);

        RoomImportRequest request = new RoomImportRequest(1L, List.of(
                RoomImportRow.of(null, "1", "A101", "Bedroom A", "1", "150", "150", "2", "AVAILABLE", null)));
        RoomImportResult result = service.importRooms(request);

        assertThat(result.roomsCreated()).isEqualTo(1);
        verify(roomService, times(1)).createRoom(any());
    }
}