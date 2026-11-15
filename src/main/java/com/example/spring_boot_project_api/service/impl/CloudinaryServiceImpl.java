package com.example.spring_boot_project_api.service.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.spring_boot_project_api.service.CloudinaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public UploadResult uploadImage(byte[] imageBytes, String folder) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image"));
            return new UploadResult(
                    (String) result.get("public_id"),
                    (String) result.get("secure_url"));
        } catch (Exception e) {
            throw new RuntimeException("Image upload to Cloudinary failed", e);
        }
    }

    @Override
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException("Image deletion from Cloudinary failed", e);
        }
    }
}