package com.example.spring_boot_project_api.repository;

/**
 * Projection for a per-house room count grouped by {@code houseId}. Used to
 * derive house room totals/available counts without loading every room.
 */
public interface HouseRoomCountProjection {

    Long getHouseId();

    Long getRoomCount();
}
