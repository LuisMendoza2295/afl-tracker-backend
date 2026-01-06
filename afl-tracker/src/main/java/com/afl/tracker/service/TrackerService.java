package com.afl.tracker.service;

import java.nio.file.Files;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

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

    public BlobInfo uploadFile(FileUpload file) {
        try {
            byte[] content = Files.readAllBytes(file.uploadedFile());

            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, file.fileName())
                .setContentType(file.contentType())
                .build();
            var uploaded = storage.create(blobInfo, content);

            return uploaded.asBlobInfo();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("File " + file.fileName() + " upload failed", e);
        }
    }
}
