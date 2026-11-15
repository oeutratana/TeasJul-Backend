package com.example.spring_boot_project_api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;

class RoomTest {

    @Test
    void newRoomDefaultsToDraftPublicationStatus() {
        Room room = new Room();
        assertThat(room.getPublicationStatus()).isEqualTo(RoomPublicationStatus.DRAFT);
    }

    @Test
    void newRoomDefaultsToAvailableAvailabilityStatus() {
        Room room = new Room();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    void newRoomHasNoListingFieldsSet() {
        Room room = new Room();
        assertThat(room.getPropertyType()).isNull();
        assertThat(room.getListingScope()).isNull();
        assertThat(room.getFurnishing()).isNull();
        assertThat(room.getAvailableFrom()).isNull();
        assertThat(room.getAdvancePayment()).isNull();
        assertThat(room.getBedrooms()).isNull();
        assertThat(room.getBathrooms()).isNull();
        assertThat(room.getAreaSqm()).isNull();
        assertThat(room.getLandSizeSqm()).isNull();
        assertThat(room.getNumberOfFloors()).isNull();
    }

    @Test
    void roomStoresListingFields() {
        Room room = new Room();
        room.setPropertyType(PropertyType.ROOM);
        room.setListingScope(ListingScope.MULTIPLE_ROOMS);
        room.setFurnishing(Furnishing.SEMI_FURNISHED);
        room.setAvailableFrom(LocalDate.of(2026, 10, 1));
        room.setAdvancePayment(new BigDecimal("150.00"));
        room.setBedrooms(1);
        room.setBathrooms(1);
        room.setAreaSqm(new BigDecimal("22.50"));
        room.setLandSizeSqm(new BigDecimal("10.00"));
        room.setNumberOfFloors(1);

        assertThat(room.getPropertyType()).isEqualTo(PropertyType.ROOM);
        assertThat(room.getListingScope()).isEqualTo(ListingScope.MULTIPLE_ROOMS);
        assertThat(room.getFurnishing()).isEqualTo(Furnishing.SEMI_FURNISHED);
        assertThat(room.getAvailableFrom()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(room.getAdvancePayment()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(room.getBedrooms()).isEqualTo(1);
        assertThat(room.getBathrooms()).isEqualTo(1);
        assertThat(room.getAreaSqm()).isEqualByComparingTo(new BigDecimal("22.50"));
        assertThat(room.getLandSizeSqm()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(room.getNumberOfFloors()).isEqualTo(1);
    }
}