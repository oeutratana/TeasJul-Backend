package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.spring_boot_project_api.enums.RoomBookingStatus;
import com.example.spring_boot_project_api.model.RoomBooking;

public interface RoomBookingRepository extends JpaRepository<RoomBooking, Long> {
    List<RoomBooking> findByRoomIdInOrderByCreatedAtDesc(Collection<Long> roomIds);
    Optional<RoomBooking> findFirstByRoomIdAndStatusOrderByCreatedAtDesc(Long roomId, RoomBookingStatus status);
}
