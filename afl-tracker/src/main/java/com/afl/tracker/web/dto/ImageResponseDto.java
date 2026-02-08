package com.afl.tracker.web.dto;

import java.time.Instant;

import com.afl.tracker.domain.model.ImageInfo;

public class ImageResponseDto {
    private String id;
    private String url;
    private String latitude;
    private String longitude;
    private String uploadedByName;
    private String uploadedByEmail;
    private Instant uploadedAt;

    public static ImageResponseDto from(ImageInfo imageInfo) {
        ImageResponseDto dto = new ImageResponseDto();
        dto.id = imageInfo.id();
        dto.url = imageInfo.url();
        
        if (imageInfo.location() != null) {
            dto.latitude = imageInfo.location().latitude().toString();
            dto.longitude = imageInfo.location().longitude().toString();
        }
        
        if (imageInfo.uploadedBy() != null) {
            dto.uploadedByName = imageInfo.uploadedBy().name();
            dto.uploadedByEmail = imageInfo.uploadedBy().email();
        }
        
        if (imageInfo.uploadedAt() != null) {
            dto.uploadedAt = imageInfo.uploadedAt();
        }
        
        return dto;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getUploadedByName() {
        return uploadedByName;
    }

    public String getUploadedByEmail() {
        return uploadedByEmail;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
