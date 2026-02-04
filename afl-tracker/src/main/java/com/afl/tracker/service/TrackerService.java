package com.afl.tracker.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TrackerService {

    @Inject
    Logger logger;

    @Inject
    Storage storage;

    @Inject
    VisionService visionService;

    @ConfigProperty(name = "application.gcs.bucket-name")
    String bucketName;

    private static final String STORAGE_GOOGLEAPIS_BASE_URL = "https://storage.googleapis.com";

    public List<String> getImages() {
        return StreamSupport.stream(storage.list(bucketName).iterateAll().spliterator(), false)
                .map(blob -> {
                    return String.format("%s/%s/%s", STORAGE_GOOGLEAPIS_BASE_URL, bucketName, blob.getName());
                })
                .toList();
    }

    public BlobInfo uploadFile(FileUpload file) {
        try {
            // Validate logo presence before uploading
            boolean logoDetected = visionService.validateLogoPresence(file.uploadedFile(), file.fileName());
            if (!logoDetected) {
                throw new IllegalArgumentException("Logo not detected in image. Please upload an image containing the AFL logo.");
            }

            String fileUUID = UUID.randomUUID().toString();
            BlobId blobId = BlobId.of(bucketName, fileUUID);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.contentType())
                    .setMetadata(Map.of("originalName", file.fileName()))
                    .build();
            var uploaded = storage.createFrom(blobInfo, file.uploadedFile());

            return uploaded.asBlobInfo();
        } catch (IOException e) {
            logger.error("Error uploading file", e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    public Blob downloadFile(String uuid) {
        Blob blob = storage.get(BlobId.of(bucketName, uuid));
        return blob;
    }
}
