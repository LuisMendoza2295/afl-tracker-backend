package com.afl.tracker.domain.aggregate;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import com.afl.tracker.domain.model.ImageInfo;
import com.afl.tracker.domain.model.UserInfo;
import com.afl.tracker.domain.model.VisionInfo;
import com.afl.tracker.domain.model.valueobj.Location;
import com.afl.tracker.domain.port.AuthPort;
import com.afl.tracker.domain.port.DatastorePort;
import com.afl.tracker.domain.port.VisionPort;
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
    VisionPort visionPort;
    
    @Inject
    AuthPort authPort;

    @Inject
    DatastorePort datastorePort;

    @ConfigProperty(name = "application.gcs.bucket-name")
    String bucketName;

    private static final String STORAGE_GOOGLEAPIS_BASE_URL = "https://storage.googleapis.com";

    public List<String> getImages() {
        return StreamSupport.stream(storage.list(bucketName).iterateAll().spliterator(), false)
                .map(blob -> getPublicUrl(blob.getName()))
                .toList();
    }

    public ImageInfo uploadFile(UserInfo uploadedBy, Location location, FileUpload file) {
        // Get vision analysis
        VisionInfo visionInfo = visionPort.getVisionInfo(file.uploadedFile());
        
        // Validate logo presence using domain logic
        if (!visionInfo.isLogo()) {
            logger.warn(String.format("Logo not detected in %s (confidence: %.2f%%)",
                    file.fileName(), visionInfo.confidence() * 100));
            throw new IllegalArgumentException("Logo not detected in image. Please upload an image containing the AFL logo.");
        }

        try {
            String fileUUID = UUID.randomUUID().toString();
            BlobId blobId = BlobId.of(bucketName, fileUUID);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.contentType())
                    .setMetadata(Map.of("originalName", file.fileName()))
                    .build();
            var uploaded = storage.createFrom(blobInfo, file.uploadedFile());

            return saveImage(uploadedBy, location, uploaded);
        } catch (IOException e) {
            logger.error("Error uploading file", e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    public ImageInfo saveImage(UserInfo uploadedBy, Location location, BlobInfo blobInfo) {
        ImageInfo imageInfo = new ImageInfo(
            "",
            getPublicUrl(blobInfo.getName()),
            location,
            uploadedBy,
            Instant.now(),
            blobInfo.getName(),
            blobInfo.getSize()
        );
        return datastorePort.saveImage(imageInfo);
    }

    public ImageInfo getImageById(String id) {
      return datastorePort.getImageById(id);
    }

    public List<ImageInfo> getAllImages() {
      return datastorePort.getAllImages();
    }

    public VisionInfo getVisionInfo(FileUpload file) {   
        return visionPort.getVisionInfo(file.uploadedFile());
    }
    
    public String getPublicUrl(String blobName) {
        return String.format("%s/%s/%s", STORAGE_GOOGLEAPIS_BASE_URL, bucketName, blobName);
    }
}
