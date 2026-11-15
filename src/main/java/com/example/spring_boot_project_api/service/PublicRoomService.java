package com.example.spring_boot_project_api.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.PublicRoomQueryRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.PublicRoomResponse;
import com.example.spring_boot_project_api.dto.response.PublicRoomTypeResponse;

/**
 * Read-only access for the public website. Only PUBLISHED rooms are exposed.
 */
public interface PublicRoomService {

    /** Paginated list of published, available rooms with optional search and filters. */
    PageResponse<PublicRoomResponse> listRooms(PublicRoomQueryRequest query, Pageable pageable);

    /** Paginated list of published, available rooms with optional filters (legacy overload). */
    default PageResponse<PublicRoomResponse> listRooms(String search, BigDecimal minRent,
            BigDecimal maxRent, Pageable pageable) {
        return listRooms(new PublicRoomQueryRequest(search, null, null, null, null, null,
                minRent, maxRent, null, null, null, null, null, null, null, null, null), pageable);
    }

    /** Full public details of a single published room. */
    PublicRoomResponse getRoom(Long id);

    /** Get up to 4 similar published rooms from other houses. */
    List<PublicRoomResponse> getSimilarRooms(Long roomId);

    /** Distinct room types appearing in the public listing, sorted by name. */
    List<PublicRoomTypeResponse> getPublicRoomTypes();
}
