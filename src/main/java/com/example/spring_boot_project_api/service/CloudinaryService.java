package com.example.spring_boot_project_api.service;

/**
 * Thin wrapper around the Cloudinary client so that image hosting calls stay
 * mockable in unit tests and isolated from business logic.
 */
public interface CloudinaryService {

    record UploadResult(String publicId, String secureUrl) {
    }

    /**
     * Uploads an image to the given Cloudinary folder.
     *
     * @param imageBytes the raw image bytes (already validated)
     * @param folder     the Cloudinary folder, e.g. {@code teasjul/houses/3}
     * @return the public id and secure url of the uploaded asset
     */
    UploadResult uploadImage(byte[] imageBytes, String folder);

    /**
     * Deletes a previously uploaded asset by its public id.
     *
     * @param publicId the Cloudinary public id of the asset
     */
    void deleteImage(String publicId);
}