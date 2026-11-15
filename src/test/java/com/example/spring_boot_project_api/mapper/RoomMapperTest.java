package com.example.spring_boot_project_api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.example.spring_boot_project_api.dto.request.RoomRepuest;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.model.Room;

class RoomMapperTest {

    private final RoomMapper mapper = new RoomMapper();

    @Test
    void toEntityCopiesListingFields() {
        RoomRepuest request = new RoomRepuest();
        request.setPropertyType(PropertyType.ROOM);
        request.setListingScope(ListingScope.INDIVIDUAL_ROOM);
        request.setFurnishing(Furnishing.UNFURNISHED);
        request.setAvailableFrom(LocalDate.of(2026, 9, 1));
        request.setAdvancePayment(new BigDecimal("200.00"));
        request.setBedrooms(1);
        request.setBathrooms(1);
        request.setAreaSqm(new BigDecimal("18.50"));
        request.setLandSizeSqm(new BigDecimal("5.00"));
        request.setNumberOfFloors(1);

        Room room = mapper.toEntity(request);

        assertThat(room.getPropertyType()).isEqualTo(PropertyType.ROOM);
        assertThat(room.getListingScope()).isEqualTo(ListingScope.INDIVIDUAL_ROOM);
        assertThat(room.getFurnishing()).isEqualTo(Furnishing.UNFURNISHED);
        assertThat(room.getAvailableFrom()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(room.getAdvancePayment()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(room.getBedrooms()).isEqualTo(1);
        assertThat(room.getBathrooms()).isEqualTo(1);
        assertThat(room.getAreaSqm()).isEqualByComparingTo(new BigDecimal("18.50"));
        assertThat(room.getLandSizeSqm()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(room.getNumberOfFloors()).isEqualTo(1);
    }

    @Test
    void toDtoCopiesListingFields() {
        Room room = new Room();
        room.setId(1L);
        room.setPropertyType(PropertyType.APARTMENT);
        room.setListingScope(ListingScope.MULTIPLE_ROOMS);
        room.setFurnishing(Furnishing.FULLY_FURNISHED);
        room.setAvailableFrom(LocalDate.of(2026, 11, 1));
        room.setAdvancePayment(new BigDecimal("300.00"));
        room.setBedrooms(2);
        room.setBathrooms(2);
        room.setAreaSqm(new BigDecimal("60.00"));
        room.setLandSizeSqm(new BigDecimal("25.00"));
        room.setNumberOfFloors(3);

        RoomResponse response = mapper.toDto(room);

        assertThat(response.getPropertyType()).isEqualTo(PropertyType.APARTMENT);
        assertThat(response.getListingScope()).isEqualTo(ListingScope.MULTIPLE_ROOMS);
        assertThat(response.getFurnishing()).isEqualTo(Furnishing.FULLY_FURNISHED);
        assertThat(response.getAvailableFrom()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(response.getAdvancePayment()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.getBedrooms()).isEqualTo(2);
        assertThat(response.getBathrooms()).isEqualTo(2);
        assertThat(response.getAreaSqm()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(response.getLandSizeSqm()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(response.getNumberOfFloors()).isEqualTo(3);
    }

    @Test
    void toEntityWithNullRequestReturnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDtoWithNullEntityReturnsNull() {
        assertThat(mapper.toDto((Room) null)).isNull();
    }
}