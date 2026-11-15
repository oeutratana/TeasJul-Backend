package com.example.spring_boot_project_api.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploaded image files. Only JPEG/JPG, PNG and WEBP images up to 5 MB
 * are accepted. Validation covers the declared MIME type, the file size and the
 * magic bytes / file signature so that renamed non-image files are rejected.
 */
@Component
public class ImageValidationUtil {

    public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file must not be empty");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image size must not exceed 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPEG, PNG and WEBP images are allowed");
        }

        try (InputStream in = file.getInputStream()) {
            requireSupportedSignature(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read image file", e);
        }
    }

    private void requireSupportedSignature(InputStream in) throws IOException {
        byte[] header = in.readNBytes(12);
        if (isJpeg(header) || isPng(header) || isWebp(header)) {
            return;
        }
        throw new IllegalArgumentException("Unsupported image file signature");
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        byte[] signature = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebp(byte[] header) {
        if (header.length < 12) {
            return false;
        }
        String riff = new String(header, 0, 4, StandardCharsets.US_ASCII);
        String webp = new String(header, 8, 4, StandardCharsets.US_ASCII);
        return "RIFF".equals(riff) && "WEBP".equals(webp);
    }
}