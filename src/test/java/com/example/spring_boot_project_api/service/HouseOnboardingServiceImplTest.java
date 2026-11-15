package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.spring_boot_project_api.dto.request.HouseImportRequest;
import com.example.spring_boot_project_api.dto.request.HouseImportRowRequest;
import com.example.spring_boot_project_api.dto.response.HouseImportResult;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.HouseImportValidationException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.mapper.TenantMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseStatus;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.impl.HouseOnboardingServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HouseOnboardingServiceImplTest {

    @Mock
    private HouseRepository houseRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private RentalContractRepository rentalContractRepository;
    @Mock
    private ContractTenantRepository contractTenantRepository;
    @Mock
    private UserHouseAccessRepository userHouseAccessRepository;
    @Spy
    private TenantMapper tenantMapper = new TenantMapper();
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private HouseOnboardingServiceImpl service;

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
                .id(3L)
                .owner(owner)
                .name("Sunrise Residence")
                .slug("sunrise-residence")
                .code("SR001")
                .address("#123, Street 271")
                .totalRooms(0)
                .availableRooms(0)
                .status(HouseStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 8, 23, 22, 55, 25))
                .updatedAt(LocalDateTime.of(2026, 8, 23, 22, 55, 25))
                .build();
    }

    private void authenticateAsOwner() {
        when(securityUtils.getCurrentUserId()).thenReturn(15L);
    }

    private void stubHouseAndManage() {
        authenticateAsOwner();
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
    }

    private RoomType roomType(Long id) {
        RoomType rt = new RoomType();
        rt.setId(id);
        rt.setOwnerId(15L);
        rt.setName("Standard");
        rt.setCode("STD");
        rt.setCapacity(1);
        return rt;
    }

    private void stubRoomType() {
        when(roomTypeRepository.findByOwnerId(15L)).thenReturn(List.of(roomType(1L)));
    }

    private void stubContractGeneration() {
        when(rentalContractRepository.findByOwnerId(15L)).thenReturn(List.of());
        when(rentalContractRepository.findByOwnerIdAndContractNo(eq(15L), any()))
                .thenReturn(Optional.empty());
    }

    private void stubTenantGeneration() {
        when(tenantRepository.findAllByOwnerIdOrderByCreatedAtDesc(15L)).thenReturn(List.of());
        when(tenantRepository.existsByOwnerIdAndTenantCode(eq(15L), any())).thenReturn(false);
    }

    private HouseImportRowRequest validRow() {
        return HouseImportRowRequest.of("A101", "John Doe", "012111222", "john@example.com",
                "01 Jan 2026", "150", "150", "ACTIVE", 5, null);
    }

    private HouseImportRequest singleRowRequest(HouseImportRowRequest row) {
        return new HouseImportRequest(List.of(row));
    }

    // ------------------------------------------------------------------
    // Authorization
    // ------------------------------------------------------------------

    @Test
    void previewDeniesUserWithoutAccess() {
        when(securityUtils.getCurrentUserId()).thenReturn(20L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));

        assertThatThrownBy(() -> service.previewImport(3L, "x.xlsx", new byte[]{1}))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void importAllowsOwner() {
        stubHouseAndManage();
        when(roomRepository.findByHouseIdAndRoomNumber(3L, "A101")).thenReturn(Optional.empty());
        stubRoomType();
        when(tenantRepository.findByOwnerIdAndPhone(15L, "012111222")).thenReturn(Optional.empty());
        stubTenantGeneration();
        stubContractGeneration();
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room w = inv.getArgument(0);
            w.setId(1L);
            return w;
        });
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(50L);
            return t;
        });
        when(rentalContractRepository.save(any(RentalContract.class))).thenAnswer(inv -> {
            RentalContract c = inv.getArgument(0);
            c.setId(900L);
            return c;
        });
        when(contractTenantRepository.save(any(ContractTenant.class))).thenAnswer(inv -> inv.getArgument(0));

        HouseImportResult result = service.importExisting(3L, singleRowRequest(validRow()));

        assertThat(result.roomsCreated()).isEqualTo(1);
        assertThat(result.tenantsCreated()).isEqualTo(1);
        assertThat(result.contractsCreated()).isEqualTo(1);
        assertThat(result.contractTenantsCreated()).isEqualTo(1);
        verify(roomRepository, times(2)).save(any(Room.class));
        verify(tenantRepository, times(2)).save(any(Tenant.class));
        verify(rentalContractRepository, times(1)).save(any(RentalContract.class));
        verify(contractTenantRepository, times(1)).save(any(ContractTenant.class));
    }

    // ------------------------------------------------------------------
    // Import validation errors
    // ------------------------------------------------------------------

    @Test
    void importRejectsInvalidRowsWithoutWriting() {
        stubHouseAndManage();
        when(roomRepository.findByHouseIdAndRoomNumber(3L, "A101")).thenReturn(Optional.empty());
        stubRoomType();

        HouseImportRowRequest badDate = HouseImportRowRequest.of("A101", "John", "012111222",
                "john@example.com", "not-a-date", "150", "150", "ACTIVE", 5, null);

        assertThatThrownBy(() -> service.importExisting(3L, singleRowRequest(badDate)))
                .isInstanceOf(HouseImportValidationException.class);

        verify(roomRepository, never()).save(any(Room.class));
        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(rentalContractRepository, never()).save(any(RentalContract.class));
        verify(contractTenantRepository, never()).save(any(ContractTenant.class));
    }

    @Test
    void importRejectsDuplicateRoomInFile() {
        stubHouseAndManage();
        when(roomRepository.findByHouseIdAndRoomNumber(eq(3L), any())).thenReturn(Optional.empty());
        stubRoomType();
        when(tenantRepository.findByOwnerIdAndPhone(eq(15L), any())).thenReturn(Optional.empty());
        stubTenantGeneration();

        HouseImportRowRequest r1 = validRow();
        HouseImportRowRequest r2 = HouseImportRowRequest.of("A101", "Second", "012999000",
                "second@example.com", "2026-03-01", "180", "0", "ACTIVE", 5, null);

        assertThatThrownBy(() -> service.importExisting(3L, new HouseImportRequest(List.of(r1, r2))))
                .isInstanceOf(HouseImportValidationException.class)
                .satisfies(ex -> {
                    var v = ((HouseImportValidationException) ex).getRowErrors();
                    assertThat(v).containsKey(3);
                });
    }

    @Test
    void importRejectsNegativeRentForAnyRow() {
        stubHouseAndManage();
        when(roomRepository.findByHouseIdAndRoomNumber(3L, "A101")).thenReturn(Optional.empty());
        stubRoomType();

        HouseImportRowRequest neg = HouseImportRowRequest.of("A101", "John", "012111222",
                "john@example.com", "2026-03-01", "-10", "0", "ACTIVE", 5, null);

        assertThatThrownBy(() -> service.importExisting(3L, singleRowRequest(neg)))
                .isInstanceOf(HouseImportValidationException.class);
    }

    @Test
    void importRejectsInvalidStatus() {
        stubHouseAndManage();
        when(roomRepository.findByHouseIdAndRoomNumber(3L, "A101")).thenReturn(Optional.empty());
        stubRoomType();

        HouseImportRowRequest bad = HouseImportRowRequest.of("A101", "John", "012111222",
                "john@example.com", "2026-03-01", "150", "0", "UNKNOWN", 5, null);

        assertThatThrownBy(() -> service.importExisting(3L, singleRowRequest(bad)))
                .isInstanceOf(HouseImportValidationException.class);
    }

    @Test
    void importAllowsMissingOptionalFields() {
        stubHouseAndManage();
        when(roomRepository.findByHouseIdAndRoomNumber(3L, "A101")).thenReturn(Optional.empty());
        stubRoomType();
        when(tenantRepository.findByOwnerIdAndPhone(15L, "012111222")).thenReturn(Optional.empty());
        stubTenantGeneration();
        stubContractGeneration();
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room w = inv.getArgument(0);
            w.setId(1L);
            return w;
        });
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(50L);
            return t;
        });
        when(rentalContractRepository.save(any(RentalContract.class))).thenAnswer(inv -> {
            RentalContract c = inv.getArgument(0);
            c.setId(900L);
            return c;
        });
        when(contractTenantRepository.save(any(ContractTenant.class))).thenAnswer(inv -> inv.getArgument(0));

        HouseImportRowRequest partial = HouseImportRowRequest.of("A101", "John Doe", "012111222",
                null, "2026-03-01", "150", null, null, null, null);

        HouseImportResult result = service.importExisting(3L, singleRowRequest(partial));

        assertThat(result.roomsCreated()).isEqualTo(1);
        assertThat(result.tenantsCreated()).isEqualTo(1);
        assertThat(result.contractsCreated()).isEqualTo(1);
    }

    @Test
    void importDuplicatePhoneIsWarningNotError() {
        stubHouseAndManage();
        when(roomRepository.findByHouseIdAndRoomNumber(eq(3L), any())).thenReturn(Optional.empty());
        stubRoomType();
        when(tenantRepository.findByOwnerIdAndPhone(eq(15L), any())).thenReturn(Optional.empty());
        stubTenantGeneration();
        stubContractGeneration();
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room w = inv.getArgument(0);
            w.setId(w.getRoomNumber().equals("A101") ? 1L : 2L);
            return w;
        });
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(50L);
            return t;
        });
        when(rentalContractRepository.save(any(RentalContract.class))).thenAnswer(inv -> {
            RentalContract c = inv.getArgument(0);
            c.setId(900L);
            return c;
        });
        when(contractTenantRepository.save(any(ContractTenant.class))).thenAnswer(inv -> inv.getArgument(0));

        HouseImportRowRequest r1 = validRow();
        HouseImportRowRequest r2 = HouseImportRowRequest.of("B202", "John Duplicate", "012111222",
                "dup@example.com", "2026-03-01", "160", "0", "ACTIVE", 5, null);

        HouseImportResult result = service.importExisting(3L, new HouseImportRequest(List.of(r1, r2)));

        assertThat(result.contractsCreated()).isEqualTo(2);
    }

    // ------------------------------------------------------------------
    // Missing house / empty file
    // ------------------------------------------------------------------

    @Test
    void parseImportRejectsEmptyContent() {
        when(securityUtils.getCurrentUserId()).thenReturn(15L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));

        assertThatThrownBy(() -> service.parseImport(3L, new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void parseImportRejectsMissingHouse() {
        when(securityUtils.getCurrentUserId()).thenReturn(15L);
        when(houseRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.parseImport(3L, new byte[]{1}))
                .isInstanceOf(HouseNotFoundException.class);
    }

    @Test
    void parseImportParsesCsvTemplate() {
        stubHouseAndManage();
        String csv = "RoomNo,TenantName,Phone,Email,MoveInDate,MonthlyRent,Deposit,ContractStatus,DueDay,Notes\n"
                + "A101,John Doe,012111222,john@example.com,01 Jan 2026,150,150,ACTIVE,5,\n"
                + "A102,Jane Smith,098765432,jane@example.com,01 Feb 2026,180,0,ACTIVE,5,\n";

        List<HouseImportRowRequest> rows = service.parseImport(3L, csv.getBytes());

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).roomNo()).isEqualTo("A101");
        assertThat(rows.get(0).tenantName()).isEqualTo("John Doe");
        assertThat(rows.get(0).moveInDate()).isEqualTo("01 Jan 2026");
        assertThat(rows.get(0).rent()).isEqualTo("150");
        assertThat(rows.get(0).deposit()).isEqualTo("150");
        assertThat(rows.get(0).dueDay()).isEqualTo(5);
        assertThat(rows.get(1).roomNo()).isEqualTo("A102");
        assertThat(rows.get(1).dueDay()).isEqualTo(5);
    }

    @Test
    void parseImportParsesCsvWithQuotedFieldsAndBom() {
        stubHouseAndManage();
        String csv = "\uFEFFRoomNo,TenantName,Phone,Email,MoveInDate,MonthlyRent,Deposit,ContractStatus,DueDay,Notes\n"
                + "A101,\"Doe, John\",012111222,john@example.com,01 Jan 2026,150,150,ACTIVE,5,\"\"\n";

        List<HouseImportRowRequest> rows = service.parseImport(3L, csv.getBytes());

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).tenantName()).isEqualTo("Doe, John");
        assertThat(rows.get(0).notes()).isNull();
    }

    @Test
    void parseImportRejectsCsvWithoutDataRows() {
        stubHouseAndManage();
        String csv = "RoomNo,TenantName,Phone,Email,MoveInDate,MonthlyRent,Deposit,ContractStatus,DueDay,Notes\n";

        assertThatThrownBy(() -> service.parseImport(3L, csv.getBytes()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No data rows");
    }

    @Test
    void importRejectsNullRequest() {
        stubHouseAndManage();

        assertThatThrownBy(() -> service.importExisting(3L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void importRejectsEmptyRows() {
        stubHouseAndManage();

        assertThatThrownBy(() -> service.importExisting(3L, new HouseImportRequest(List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}