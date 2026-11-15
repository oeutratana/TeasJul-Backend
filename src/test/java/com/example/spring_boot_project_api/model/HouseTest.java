package com.example.spring_boot_project_api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.PropertyType;

class HouseTest {

    @Test
    void newHouseDefaultsToActiveStatus() {
        House house = new House();
        assertThat(house.getStatus()).isEqualTo(HouseStatus.ACTIVE);
    }

    @Test
    void newHouseHasNoListingFieldsSet() {
        House house = new House();
        assertThat(house.getPropertyType()).isNull();
        assertThat(house.getFurnishing()).isNull();
        assertThat(house.getAvailableFrom()).isNull();
        assertThat(house.getAdvancePayment()).isNull();
        assertThat(house.getBedrooms()).isNull();
        assertThat(house.getBathrooms()).isNull();
        assertThat(house.getAreaSqm()).isNull();
        assertThat(house.getLandSizeSqm()).isNull();
        assertThat(house.getNumberOfFloors()).isNull();
    }

    @Test
    void newHouseHasNoLocationFieldsSet() {
        House house = new House();
        assertThat(house.getCityProvince()).isNull();
        assertThat(house.getGoogleMapsUrl()).isNull();
    }

    @Test
    void houseStoresLocationFields() {
        House house = new House();
        house.setCityProvince("Phnom Penh");
        house.setGoogleMapsUrl("https://www.google.com/maps/place/Phnom+Penh");

        assertThat(house.getCityProvince()).isEqualTo("Phnom Penh");
        assertThat(house.getGoogleMapsUrl()).isEqualTo("https://www.google.com/maps/place/Phnom+Penh");
    }

    @Test
    void houseStoresListingFields() {
        House house = new House();
        house.setPropertyType(PropertyType.VILLA);
        house.setFurnishing(Furnishing.FULLY_FURNISHED);
        house.setAvailableFrom(LocalDate.of(2026, 9, 20));
        house.setAdvancePayment(new BigDecimal("500.00"));
        house.setBedrooms(4);
        house.setBathrooms(3);
        house.setAreaSqm(new BigDecimal("150.00"));
        house.setLandSizeSqm(new BigDecimal("300.00"));
        house.setNumberOfFloors(2);

        assertThat(house.getPropertyType()).isEqualTo(PropertyType.VILLA);
        assertThat(house.getFurnishing()).isEqualTo(Furnishing.FULLY_FURNISHED);
        assertThat(house.getAvailableFrom()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(house.getAdvancePayment()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(house.getBedrooms()).isEqualTo(4);
        assertThat(house.getBathrooms()).isEqualTo(3);
        assertThat(house.getAreaSqm()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(house.getLandSizeSqm()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(house.getNumberOfFloors()).isEqualTo(2);
    }
}