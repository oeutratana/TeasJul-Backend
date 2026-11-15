package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.RoomBookingRequest;
import com.example.spring_boot_project_api.dto.response.RoomBookingResponse;
import com.example.spring_boot_project_api.enums.RoomBookingStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.InvalidRoomBookingOperationException;
import com.example.spring_boot_project_api.mapper.RoomBookingMapper;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomBooking;
import com.example.spring_boot_project_api.repository.RoomBookingRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.impl.RoomBookingServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class RoomBookingServiceImplTest {
    @Mock private RoomBookingRepository bookingRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private UserHouseAccessRepository userHouseAccessRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private RoomBookingMapper bookingMapper;
    @InjectMocks private RoomBookingServiceImpl bookingService;

    @Test
    void availableRoomCanBeBookedWithoutCreatingATenant() {
        Room room = room(RoomStatus.AVAILABLE, 1L);
        RoomBookingRequest request = request();
        RoomBookingResponse response = response(10L, RoomBookingStatus.BOOKED);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(roomRepository.findWithLockById(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.save(any(RoomBooking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toResponse(any(RoomBooking.class))).thenReturn(response);

        RoomBookingResponse actual = bookingService.createBooking(request);

        assertThat(actual).isSameAs(response);
        ArgumentCaptor<RoomBooking> booking = ArgumentCaptor.forClass(RoomBooking.class);
        verify(bookingRepository).save(booking.capture());
        assertThat(booking.getValue().getRoomId()).isEqualTo(room.getId());
        assertThat(booking.getValue().getStatus()).isEqualTo(RoomBookingStatus.BOOKED);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.BOOKED);
        assertThat(Arrays.stream(RoomBooking.class.getDeclaredFields()).map(field -> field.getName()))
                .doesNotContain("tenant", "tenantId");
        verify(roomRepository).save(room);
    }

    @Test
    void bookedRoomCannotBeBookedAgain() {
        assertCannotBook(RoomStatus.BOOKED);
    }

    @Test
    void occupiedRoomCannotBeBooked() {
        assertCannotBook(RoomStatus.OCCUPIED);
    }

    @Test
    void ownerCannotBookAnotherOwnersRoom() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(roomRepository.findWithLockById(10L)).thenReturn(Optional.of(room(RoomStatus.AVAILABLE, 2L)));

        assertThatThrownBy(() -> bookingService.createBooking(request())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancellingBookingMakesBookedRoomAvailable() {
        Room room = room(RoomStatus.BOOKED, 1L);
        RoomBooking booking = new RoomBooking();
        booking.setId(5L);
        booking.setRoomId(10L);
        booking.setStatus(RoomBookingStatus.BOOKED);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toResponse(booking)).thenReturn(response(5L, RoomBookingStatus.CANCELLED));

        bookingService.cancelBooking(5L);

        assertThat(booking.getStatus()).isEqualTo(RoomBookingStatus.CANCELLED);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        verify(roomRepository).save(room);
    }

    @Test
    void completingBookingForContractCompletesOnlyTheSpecifiedCurrentBooking() {
        RoomBooking booking = new RoomBooking();
        booking.setId(5L);
        booking.setRoomId(10L);
        booking.setStatus(RoomBookingStatus.BOOKED);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        bookingService.completeBookingForContract(5L, 10L);

        assertThat(booking.getStatus()).isEqualTo(RoomBookingStatus.COMPLETED);
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelledBookingCannotBeCompletedForContract() {
        RoomBooking booking = new RoomBooking();
        booking.setId(5L);
        booking.setRoomId(10L);
        booking.setStatus(RoomBookingStatus.CANCELLED);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.completeBookingForContract(5L, 10L))
                .isInstanceOf(InvalidRoomBookingOperationException.class);
    }

    private void assertCannotBook(RoomStatus status) {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(roomRepository.findWithLockById(10L)).thenReturn(Optional.of(room(status, 1L)));
        assertThatThrownBy(() -> bookingService.createBooking(request())).isInstanceOf(InvalidRoomBookingOperationException.class);
    }

    private static Room room(RoomStatus status, Long ownerId) {
        Room room = new Room();
        room.setId(10L);
        room.setOwnerId(ownerId);
        room.setHouseId(20L);
        room.setStatus(status);
        return room;
    }

    private static RoomBookingRequest request() {
        RoomBookingRequest request = new RoomBookingRequest();
        request.setRoomId(10L);
        request.setCustomerName("Jane Customer");
        request.setCustomerPhone("012345678");
        request.setExpectedMoveInDate(LocalDate.now().plusDays(1));
        return request;
    }

    private static RoomBookingResponse response(Long id, RoomBookingStatus status) {
        return new RoomBookingResponse(id, 10L, "Jane Customer", "012345678", null,
                LocalDate.now(), LocalDate.now().plusDays(1), status, null, null, null);
    }
}
