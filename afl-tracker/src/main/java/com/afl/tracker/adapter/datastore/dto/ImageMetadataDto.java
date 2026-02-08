package com.afl.tracker.adapter.datastore.dto;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;

public class ImageMetadataDto {
    @DocumentId
    private String id;
    private String url;
    private LocationDto location;
    private UserInfoDto uploadedBy;
    private Timestamp uploadedAt;
    private String fileName;
    private Long fileSize;

    public ImageMetadataDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }

    public UserInfoDto getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UserInfoDto uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

}
