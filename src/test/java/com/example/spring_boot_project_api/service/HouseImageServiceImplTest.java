package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.response.HouseImageResponse;
import com.example.spring_boot_project_api.mapper.HouseImageMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseImage;
import com.example.spring_boot_project_api.repository.HouseImageRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.service.impl.HouseImageServiceImpl;
import com.example.spring_boot_project_api.util.ImageValidationUtil;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class HouseImageServiceImplTest {

    private static final Long HOUSE_ID = 5L;

    @Mock
    private HouseImageRepository houseImageRepository;
    @Mock
    private HouseRepository houseRepository;
    @Mock
    private HouseAccessService houseAccessService;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private ImageValidationUtil imageValidationUtil;

    @Spy
    private HouseImageMapper houseImageMapper = new HouseImageMapper();

    @InjectMocks
    private HouseImageServiceImpl houseImageService;

    private HouseImage image(Long id, String publicId, boolean primary, int order) {
        return HouseImage.builder()
                .id(id)
                .house(House.builder().id(HOUSE_ID).build())
                .publicId(publicId)
                .secureUrl("https://cdn/" + publicId + ".jpg")
                .isPrimary(primary)
                .sortOrder(order)
                .build();
    }

    private MultipartFile file(String name) {
        return new MockMultipartFile("files", name, "image/jpeg", new byte[]{1, 2, 3});
    }

    @Test
    void getImages_requiresHouseAccess() {
        doThrow(new AccessDeniedException("no access"))
                .when(houseAccessService).requireHouseAccess(HOUSE_ID);

        assertThatThrownBy(() -> houseImageService.getImages(HOUSE_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(houseImageRepository, never())
                .findByHouseIdOrderBySortOrderAscIdAsc(any());
    }

    @Test
    void upload_whenHouseHasNoImages_marksFirstUploadPrimary() {
        when(houseRepository.existsById(HOUSE_ID)).thenReturn(true);
        when(houseRepository.getReferenceById(HOUSE_ID)).thenReturn(House.builder().id(HOUSE_ID).build());
        when(houseImageRepository.findByHouseIdOrderBySortOrderAscIdAsc(HOUSE_ID))
                .thenReturn(List.of())
                .thenAnswer(inv -> savedImages);
        when(cloudinaryService.uploadImage(any(byte[].class), eq("teasjul/houses/" + HOUSE_ID)))
                .thenReturn(new CloudinaryService.UploadResult("pub-1", "https://cdn/pub-1.jpg"));
        doAnswer(inv -> {
            savedImages.addAll(inv.getArgument(0));
            return inv.getArgument(0);
        }).when(houseImageRepository).saveAll(anyList());

        List<HouseImageResponse> result = houseImageService.upload(HOUSE_ID, new MultipartFile[]{file("a.jpg")});

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPrimary()).isTrue();
        assertThat(result.get(0).sortOrder()).isZero();
        assertThat(result.get(0).secureUrl()).isEqualTo("https://cdn/pub-1.jpg");
        verify(imageValidationUtil).validate(any(MultipartFile.class));
    }

    @Test
    void upload_whenImagesExist_doesNotOverridePrimaryAndAppendsOrder() {
        when(houseRepository.existsById(HOUSE_ID)).thenReturn(true);
        when(houseRepository.getReferenceById(HOUSE_ID)).thenReturn(House.builder().id(HOUSE_ID).build());
        when(houseImageRepository.findByHouseIdOrderBySortOrderAscIdAsc(HOUSE_ID))
                .thenReturn(List.of(image(1L, "pub-1", true, 0), image(2L, "pub-2", false, 1)))
                .thenAnswer(inv -> savedImages);
        when(cloudinaryService.uploadImage(any(byte[].class), any()))
                .thenReturn(new CloudinaryService.UploadResult("pub-3", "https://cdn/pub-3.jpg"));
        savedImages.add(image(1L, "pub-1", true, 0));
        savedImages.add(image(2L, "pub-2", false, 1));
        doAnswer(inv -> {
            savedImages.addAll(inv.getArgument(0));
            return inv.getArgument(0);
        }).when(houseImageRepository).saveAll(anyList());

        houseImageService.upload(HOUSE_ID, new MultipartFile[]{file("c.jpg")});

        assertThat(savedImages).hasSize(3);
        assertThat(savedImages.get(2).getIsPrimary()).isFalse();
        assertThat(savedImages.get(2).getSortOrder()).isEqualTo(2);
    }

    @Test
    void setPrimary_clearsPreviousPrimary() {
        HouseImage first = image(1L, "pub-1", true, 0);
        HouseImage second = image(2L, "pub-2", false, 1);
        when(houseImageRepository.findByIdAndHouseId(2L, HOUSE_ID)).thenReturn(Optional.of(second));
        when(houseImageRepository.findByHouseIdOrderBySortOrderAscIdAsc(HOUSE_ID))
                .thenReturn(List.of(first, second));

        List<HouseImageResponse> result = houseImageService.setPrimary(HOUSE_ID, 2L);

        assertThat(result).filteredOn(HouseImageResponse::isPrimary).hasSize(1);
        assertThat(first.getIsPrimary()).isFalse();
        assertThat(second.getIsPrimary()).isTrue();
        verify(houseImageRepository).saveAll(anyList());
    }

    @Test
    void updateOrder_movesImageToRequestedPositionAndReindexes() {
        HouseImage a = image(1L, "pub-1", true, 0);
        HouseImage b = image(2L, "pub-2", false, 1);
        HouseImage c = image(3L, "pub-3", false, 2);
        when(houseImageRepository.findByIdAndHouseId(1L, HOUSE_ID)).thenReturn(Optional.of(a));
        when(houseImageRepository.findByHouseIdOrderBySortOrderAscIdAsc(HOUSE_ID))
                .thenReturn(new ArrayList<>(List.of(a, b, c)));

        List<HouseImageResponse> result = houseImageService.updateOrder(HOUSE_ID, 1L, 2);

        assertThat(result).extracting(HouseImageResponse::id).containsExactly(2L, 3L, 1L);
        assertThat(result).extracting(HouseImageResponse::sortOrder).containsExactly(0, 1, 2);
    }

    @Test
    void delete_whenPrimary_reassignsPrimaryToFirstRemaining() {
        HouseImage primary = image(1L, "pub-1", true, 0);
        HouseImage next = image(2L, "pub-2", false, 1);
        when(houseImageRepository.findByIdAndHouseId(1L, HOUSE_ID)).thenReturn(Optional.of(primary));
        when(houseImageRepository.findFirstByHouseIdOrderBySortOrderAscIdAsc(HOUSE_ID))
                .thenReturn(Optional.of(next));

        houseImageService.delete(HOUSE_ID, 1L);

        verify(cloudinaryService).deleteImage("pub-1");
        verify(houseImageRepository).delete(primary);
        assertThat(next.getIsPrimary()).isTrue();
        verify(houseImageRepository).save(next);
    }

    @Test
    void delete_whenCloudinaryFails_stillDeletesDatabaseRow() {
        HouseImage image = image(1L, "pub-1", false, 0);
        when(houseImageRepository.findByIdAndHouseId(1L, HOUSE_ID)).thenReturn(Optional.of(image));
        doThrow(new RuntimeException("cloudinary down")).when(cloudinaryService).deleteImage("pub-1");

        houseImageService.delete(HOUSE_ID, 1L);

        verify(houseImageRepository).delete(image);
    }

    @Test
    void upload_rejectsWhenNoFilesProvided() {
        when(houseRepository.existsById(HOUSE_ID)).thenReturn(true);

        assertThatThrownBy(() -> houseImageService.upload(HOUSE_ID, new MultipartFile[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private final List<HouseImage> savedImages = new ArrayList<>();
}