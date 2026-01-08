package com.afl.tracker.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.inject.ConfigProperty;
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
    Storage storage;

    @ConfigProperty(name = "application.gcs.bucket-name")
    String bucketName;

    public List<String> getImages() {
        return StreamSupport.stream(storage.list(bucketName).iterateAll().spliterator(), false)
                .map(Blob::getMediaLink)
                .toList();
    }

    public BlobInfo uploadFile(FileUpload file) {
        try {
            String fileUUID = UUID.randomUUID().toString();
            BlobId blobId = BlobId.of(bucketName, fileUUID);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.contentType())
                .setMetadata(Map.of("originalName", file.fileName()))
                .build();
            var uploaded = storage.createFrom(blobInfo, file.uploadedFile());

            return uploaded.asBlobInfo();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("File upload failed", e);
        }
    }

    public Blob downloadFile(String uuid) {
        Blob blob = storage.get(BlobId.of(bucketName, uuid));
        return blob;
    }
}
