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
import com.example.spring_boot_project_api.dto.response.ListingImageResponse;
import com.example.spring_boot_project_api.service.ListingImageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/listings/{listingId}/images")
@RequiredArgsConstructor
@Tag(name = "Listing Images", description = "Listing gallery management")
public class ListingImageController {

    private final ListingImageService listingImageService;

    @Operation(summary = "List listing images",
            description = "Returns all images of the requested listing ordered by display order.")
    @GetMapping
    public ResponseEntity<List<ListingImageResponse>> getImages(@PathVariable Long listingId) {
        return ResponseEntity.ok(listingImageService.getImages(listingId));
    }

    @Operation(summary = "Upload listing images",
            description = "Uploads one or more images for the requested listing. JPEG, PNG and WEBP only, max 5 MB each.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ListingImageResponse>> upload(@PathVariable Long listingId,
            @RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingImageService.upload(listingId, files));
    }

    @Operation(summary = "Set a listing image as primary",
            description = "Marks the requested image as the primary image of the listing. Only one primary image is kept.")
    @PatchMapping("/{imageId}/primary")
    public ResponseEntity<List<ListingImageResponse>> setPrimary(@PathVariable Long listingId,
            @PathVariable Long imageId) {
        return ResponseEntity.ok(listingImageService.setPrimary(listingId, imageId));
    }

    @Operation(summary = "Reorder a listing image",
            description = "Moves the requested image to the given display position. Positions are zero-based.")
    @PatchMapping("/{imageId}/order")
    public ResponseEntity<List<ListingImageResponse>> updateOrder(@PathVariable Long listingId,
            @PathVariable Long imageId, @Valid @RequestBody UpdateImageOrderRequest request) {
        return ResponseEntity.ok(listingImageService.updateOrder(listingId, imageId, request.sortOrder()));
    }

    @Operation(summary = "Delete a listing image",
            description = "Deletes the image row and its Cloudinary asset. If the deleted image was primary, the next image becomes primary.")
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long listingId, @PathVariable Long imageId) {
        listingImageService.delete(listingId, imageId);
        return ResponseEntity.noContent().build();
    }
}