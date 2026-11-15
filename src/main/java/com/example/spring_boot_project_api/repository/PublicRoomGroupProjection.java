package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;

/** Aggregate values for one publicly visible House + RoomType listing group. */
public interface PublicRoomGroupProjection {

    Long getRepresentativeRoomId();

    Long getHouseId();

    Long getRoomTypeId();

    Long getAvailableRoomCount();

    BigDecimal getFromRent();
}
