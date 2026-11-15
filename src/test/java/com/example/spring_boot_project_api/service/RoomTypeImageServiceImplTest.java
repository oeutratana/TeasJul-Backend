package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.response.RoomTypeImageResponse;
import com.example.spring_boot_project_api.mapper.RoomTypeImageMapper;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.RoomTypeImage;
import com.example.spring_boot_project_api.repository.RoomTypeImageRepository;
import com.example.spring_boot_project_api.service.impl.RoomTypeImageServiceImpl;
import com.example.spring_boot_project_api.util.ImageValidationUtil;

@ExtendWith(MockitoExtension.class)
class RoomTypeImageServiceImplTest {

    private static final Long ROOM_TYPE_ID = 9L;

    @Mock
    private RoomTypeImageRepository roomTypeImageRepository;
    @Mock
    private RoomTypeService roomTypeService;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private ImageValidationUtil imageValidationUtil;

    @Spy
    private RoomTypeImageMapper roomTypeImageMapper = new RoomTypeImageMapper();

    @InjectMocks
    private RoomTypeImageServiceImpl roomTypeImageService;

    private RoomTypeImage image(Long id, String publicId, boolean primary, int order) {
        return RoomTypeImage.builder()
                .id(id)
                .roomType(roomType())
                .publicId(publicId)
                .secureUrl("https://cdn/" + publicId + ".jpg")
                .isPrimary(primary)
                .sortOrder(order)
                .build();
    }

    private RoomType roomType() {
        RoomType roomType = new RoomType();
        roomType.setId(ROOM_TYPE_ID);
        return roomType;
    }

    private MultipartFile file(String name) {
        return new MockMultipartFile("files", name, "image/png", new byte[]{1, 2, 3});
    }

    @Test
    void getImages_rejectsWhenRoomTypeNotAccessible() {
        doThrow(new AccessDeniedException("no access"))
                .when(roomTypeService).requireAccessibleRoomType(ROOM_TYPE_ID);

        assertThatThrownBy(() -> roomTypeImageService.getImages(ROOM_TYPE_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(roomTypeImageRepository, never())
                .findByRoomTypeIdOrderBySortOrderAscIdAsc(any());
    }

    @Test
    void upload_checksOwnershipThenMarksFirstImagePrimary() {
        when(roomTypeService.requireAccessibleRoomType(ROOM_TYPE_ID))
                .thenReturn(roomType());
        when(roomTypeImageRepository.findByRoomTypeIdOrderBySortOrderAscIdAsc(ROOM_TYPE_ID))
                .thenReturn(List.of())
                .thenAnswer(inv -> savedImages);
        when(cloudinaryService.uploadImage(any(byte[].class), eq("teasjul/room-types/" + ROOM_TYPE_ID)))
                .thenReturn(new CloudinaryService.UploadResult("rt-1", "https://cdn/rt-1.jpg"));
        when(roomTypeImageRepository.saveAll(anyList()))
                .thenAnswer(inv -> {
                    savedImages.addAll(inv.getArgument(0));
                    return inv.getArgument(0);
                });

        List<RoomTypeImageResponse> result =
                roomTypeImageService.upload(ROOM_TYPE_ID, new MultipartFile[]{file("a.png")});

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPrimary()).isTrue();
        assertThat(result.get(0).sortOrder()).isZero();
    }

    @Test
    void setPrimary_clearsPreviousPrimary() {
        RoomTypeImage first = image(1L, "rt-1", true, 0);
        RoomTypeImage second = image(2L, "rt-2", false, 1);
        when(roomTypeImageRepository.findByIdAndRoomTypeId(2L, ROOM_TYPE_ID))
                .thenReturn(Optional.of(second));
        when(roomTypeImageRepository.findByRoomTypeIdOrderBySortOrderAscIdAsc(ROOM_TYPE_ID))
                .thenReturn(List.of(first, second));

        List<RoomTypeImageResponse> result = roomTypeImageService.setPrimary(ROOM_TYPE_ID, 2L);

        assertThat(result).filteredOn(RoomTypeImageResponse::isPrimary).hasSize(1);
        assertThat(first.getIsPrimary()).isFalse();
        assertThat(second.getIsPrimary()).isTrue();
        verify(roomTypeImageRepository).saveAll(anyList());
    }

    @Test
    void updateOrder_movesImageToRequestedPositionAndReindexes() {
        RoomTypeImage a = image(1L, "rt-1", true, 0);
        RoomTypeImage b = image(2L, "rt-2", false, 1);
        RoomTypeImage c = image(3L, "rt-3", false, 2);
        when(roomTypeImageRepository.findByIdAndRoomTypeId(1L, ROOM_TYPE_ID)).thenReturn(Optional.of(a));
        when(roomTypeImageRepository.findByRoomTypeIdOrderBySortOrderAscIdAsc(ROOM_TYPE_ID))
                .thenReturn(new ArrayList<>(List.of(a, b, c)));

        List<RoomTypeImageResponse> result = roomTypeImageService.updateOrder(ROOM_TYPE_ID, 1L, 2);

        assertThat(result).extracting(RoomTypeImageResponse::id).containsExactly(2L, 3L, 1L);
        assertThat(result).extracting(RoomTypeImageResponse::sortOrder).containsExactly(0, 1, 2);
    }

    @Test
    void delete_whenPrimary_reassignsPrimaryToFirstRemaining() {
        RoomTypeImage primary = image(1L, "rt-1", true, 0);
        RoomTypeImage next = image(2L, "rt-2", false, 1);
        when(roomTypeImageRepository.findByIdAndRoomTypeId(1L, ROOM_TYPE_ID))
                .thenReturn(Optional.of(primary));
        when(roomTypeImageRepository.findFirstByRoomTypeIdOrderBySortOrderAscIdAsc(ROOM_TYPE_ID))
                .thenReturn(Optional.of(next));

        roomTypeImageService.delete(ROOM_TYPE_ID, 1L);

        verify(cloudinaryService).deleteImage("rt-1");
        verify(roomTypeImageRepository).delete(primary);
        assertThat(next.getIsPrimary()).isTrue();
        verify(roomTypeImageRepository).save(next);
    }

    @Test
    void upload_rejectsWhenNoFilesProvided() {
        when(roomTypeService.requireAccessibleRoomType(ROOM_TYPE_ID))
                .thenReturn(roomType());

        assertThatThrownBy(() -> roomTypeImageService.upload(ROOM_TYPE_ID, new MultipartFile[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private final List<RoomTypeImage> savedImages = new ArrayList<>();
}