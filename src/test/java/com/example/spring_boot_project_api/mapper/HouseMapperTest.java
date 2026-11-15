package com.example.spring_boot_project_api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseStatus;
import com.example.spring_boot_project_api.model.User;

class HouseMapperTest {

    private final HouseMapper mapper = new HouseMapper();

    @Test
    void toEntityCopiesListingFields() {
        CreateHouseRequest request = new CreateHouseRequest(
                "Villa Mondul", "villa-mondul", "H001", "Phnom Penh", "12000", null, null,
                "Phnom Penh", "https://www.google.com/maps/place/Villa+Mondul", 5, 3,
                "A nice villa",
                PropertyType.VILLA, Furnishing.FULLY_FURNISHED, LocalDate.of(2026, 9, 20),
                new BigDecimal("500.00"), 4, 3, new BigDecimal("150.00"), new BigDecimal("300.00"), 2);

        House house = mapper.toEntity(request);

        assertThat(house.getPropertyType()).isEqualTo(PropertyType.VILLA);
        assertThat(house.getFurnishing()).isEqualTo(Furnishing.FULLY_FURNISHED);
        assertThat(house.getAvailableFrom()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(house.getAdvancePayment()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(house.getBedrooms()).isEqualTo(4);
        assertThat(house.getBathrooms()).isEqualTo(3);
        assertThat(house.getAreaSqm()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(house.getLandSizeSqm()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(house.getNumberOfFloors()).isEqualTo(2);
        assertThat(house.getStatus()).isEqualTo(HouseStatus.ACTIVE);
    }

    @Test
    void toEntityCopiesLocationFields() {
        CreateHouseRequest request = new CreateHouseRequest(
                "Villa Mondul", "villa-mondul", "H001", "Street 200", "12000",
                new BigDecimal("11.5564"), new BigDecimal("104.9282"),
                "Phnom Penh", "https://www.google.com/maps/place/Phnom+Penh",
                5, 3, "A nice villa", null, null, null, null, null, null, null, null, null);

        House house = mapper.toEntity(request);

        assertThat(house.getAddress()).isEqualTo("Street 200");
        assertThat(house.getPostalCode()).isEqualTo("12000");
        assertThat(house.getLatitude()).isEqualByComparingTo(new BigDecimal("11.5564"));
        assertThat(house.getLongitude()).isEqualByComparingTo(new BigDecimal("104.9282"));
        assertThat(house.getCityProvince()).isEqualTo("Phnom Penh");
        assertThat(house.getGoogleMapsUrl()).isEqualTo("https://www.google.com/maps/place/Phnom+Penh");
    }

    @Test
    void toResponseCopiesListingFields() {
        User owner = new User();
        owner.setId(7L);

        House house = House.builder()
                .id(1L)
                .owner(owner)
                .name("Villa Mondul")
                .status(HouseStatus.ACTIVE)
                .propertyType(PropertyType.VILLA)
                .furnishing(Furnishing.SEMI_FURNISHED)
                .availableFrom(LocalDate.of(2026, 10, 1))
                .advancePayment(new BigDecimal("250.00"))
                .bedrooms(3)
                .bathrooms(2)
                .areaSqm(new BigDecimal("120.00"))
                .landSizeSqm(new BigDecimal("200.00"))
                .numberOfFloors(2)
                .cityProvince("Siem Reap")
                .googleMapsUrl("https://www.google.com/maps/place/Siem+Reap")
                .build();

        HouseResponse response = mapper.toResponse(house);

        assertThat(response.ownerId()).isEqualTo(7L);
        assertThat(response.propertyType()).isEqualTo(PropertyType.VILLA);
        assertThat(response.furnishing()).isEqualTo(Furnishing.SEMI_FURNISHED);
        assertThat(response.availableFrom()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(response.advancePayment()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(response.bedrooms()).isEqualTo(3);
        assertThat(response.bathrooms()).isEqualTo(2);
        assertThat(response.areaSqm()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(response.landSizeSqm()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.numberOfFloors()).isEqualTo(2);
        assertThat(response.cityProvince()).isEqualTo("Siem Reap");
        assertThat(response.googleMapsUrl()).isEqualTo("https://www.google.com/maps/place/Siem+Reap");
    }
}