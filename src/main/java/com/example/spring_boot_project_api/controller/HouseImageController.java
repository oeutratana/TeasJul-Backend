package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.request.UpdateImageOrderRequest;
import com.example.spring_boot_project_api.dto.response.HouseImageResponse;
import com.example.spring_boot_project_api.service.HouseImageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/houses/{houseId}/images")
@RequiredArgsConstructor
@Tag(name = "House Images", description = "House image gallery management")
public class HouseImageController {

    private final HouseImageService houseImageService;

    @Operation(summary = "List house images",
            description = "Returns all images of the requested house ordered by display order.")
    @GetMapping
    public ResponseEntity<List<HouseImageResponse>> getImages(@PathVariable Long houseId) {
        return ResponseEntity.ok(houseImageService.getImages(houseId));
    }

    @Operation(summary = "Upload house images",
            description = "Uploads one or more images for the requested house. JPEG, PNG and WEBP only, max 5 MB each.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<HouseImageResponse>> upload(@PathVariable Long houseId,
            @RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(houseImageService.upload(houseId, files));
    }

    @Operation(summary = "Set a house image as primary",
            description = "Marks the requested image as the primary image of the house. Only one primary image is kept.")
    @PatchMapping("/{imageId}/primary")
    public ResponseEntity<List<HouseImageResponse>> setPrimary(@PathVariable Long houseId,
            @PathVariable Long imageId) {
        return ResponseEntity.ok(houseImageService.setPrimary(houseId, imageId));
    }

    @Operation(summary = "Reorder a house image",
            description = "Moves the requested image to the given display position. Positions are zero-based.")
    @PatchMapping("/{imageId}/order")
    public ResponseEntity<List<HouseImageResponse>> updateOrder(@PathVariable Long houseId,
            @PathVariable Long imageId, @Valid @RequestBody UpdateImageOrderRequest request) {
        return ResponseEntity.ok(houseImageService.updateOrder(houseId, imageId, request.sortOrder()));
    }

    @Operation(summary = "Delete a house image",
            description = "Deletes the image row and its Cloudinary asset. If the deleted image was primary, the next image becomes primary.")
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long houseId, @PathVariable Long imageId) {
        houseImageService.delete(houseId, imageId);
        return ResponseEntity.noContent().build();
    }
}