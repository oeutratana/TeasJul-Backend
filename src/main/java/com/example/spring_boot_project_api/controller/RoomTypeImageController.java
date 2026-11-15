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
import com.example.spring_boot_project_api.dto.response.RoomTypeImageResponse;
import com.example.spring_boot_project_api.service.RoomTypeImageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/room-types/{roomTypeId}/images")
@RequiredArgsConstructor
@Tag(name = "Room Type Images", description = "Room type image gallery management")
public class RoomTypeImageController {

    private final RoomTypeImageService roomTypeImageService;

    @Operation(summary = "List room type images",
            description = "Returns all images of the requested room type ordered by display order.")
    @GetMapping
    public ResponseEntity<List<RoomTypeImageResponse>> getImages(@PathVariable Long roomTypeId) {
        return ResponseEntity.ok(roomTypeImageService.getImages(roomTypeId));
    }

    @Operation(summary = "Upload room type images",
            description = "Uploads one or more images for the requested room type. JPEG, PNG and WEBP only, max 5 MB each.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<RoomTypeImageResponse>> upload(@PathVariable Long roomTypeId,
            @RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomTypeImageService.upload(roomTypeId, files));
    }

    @Operation(summary = "Set a room type image as primary",
            description = "Marks the requested image as the primary image of the room type. Only one primary image is kept.")
    @PatchMapping("/{imageId}/primary")
    public ResponseEntity<List<RoomTypeImageResponse>> setPrimary(@PathVariable Long roomTypeId,
            @PathVariable Long imageId) {
        return ResponseEntity.ok(roomTypeImageService.setPrimary(roomTypeId, imageId));
    }

    @Operation(summary = "Reorder a room type image",
            description = "Moves the requested image to the given display position. Positions are zero-based.")
    @PatchMapping("/{imageId}/order")
    public ResponseEntity<List<RoomTypeImageResponse>> updateOrder(@PathVariable Long roomTypeId,
            @PathVariable Long imageId, @Valid @RequestBody UpdateImageOrderRequest request) {
        return ResponseEntity.ok(roomTypeImageService.updateOrder(roomTypeId, imageId, request.sortOrder()));
    }

    @Operation(summary = "Delete a room type image",
            description = "Deletes the image row and its Cloudinary asset. If the deleted image was primary, the next image becomes primary.")
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long roomTypeId, @PathVariable Long imageId) {
        roomTypeImageService.delete(roomTypeId, imageId);
        return ResponseEntity.noContent().build();
    }
}