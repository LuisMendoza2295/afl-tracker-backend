package com.afl.tracker.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;

import com.afl.tracker.util.FileUtil;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TrackerService {

    @Inject
    Storage storage;

    @Inject
    FileUtil fileUtil;

    @Inject
    Logger log;

    @ConfigProperty(name = "application.gcs.bucket-name")
    String bucketName;

    private static final String STORAGE_GOOGLEAPIS_BASE_URL = "https://storage.googleapis.com";

    private final Mat logoDescriptors = new Mat();
    private final MatOfKeyPoint logoKeypoints = new MatOfKeyPoint();
    private final ORB orb = ORB.create();

    @PostConstruct
    void init() {
        try {
            // Load the AFL logo image and compute its keypoints and descriptors
            Path logoPath = fileUtil.extractResourceToTemp("logos/afl-logo.png");
            Mat logoImage = Imgcodecs.imread(logoPath.toString(), Imgcodecs.IMREAD_GRAYSCALE);

            // logoKeypoints = new MatOfKeyPoint();
            // logoDescriptors = new Mat();

            orb.detectAndCompute(logoImage, new Mat(), logoKeypoints, logoDescriptors);

            logoImage.release();
            Files.deleteIfExists(logoPath);
        } catch (IOException e) {
            log.error("Failed to load AFL logo image", e);
            throw new RuntimeException("Failed to load AFL logo image", e);
        }
    }

    public List<String> getImages() {
        return StreamSupport.stream(storage.list(bucketName).iterateAll().spliterator(), false)
                .map(blob -> {
                    return String.format("%s/%s/%s", STORAGE_GOOGLEAPIS_BASE_URL, bucketName, blob.getName());
                })
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

    public boolean isAFLLogo(FileUpload file) throws IOException {
        // Read the uploaded file into a Mat
        byte[] fileBytes = Files.readAllBytes(file.uploadedFile());
        Mat inputImage = Imgcodecs.imdecode(new MatOfByte(fileBytes), Imgcodecs.IMREAD_GRAYSCALE);

        Mat inputDescriptor = new Mat();
        MatOfKeyPoint inputKeypoints = new MatOfKeyPoint();

        orb.detectAndCompute(inputImage, new Mat(), inputKeypoints, inputDescriptor);

        // Simple matching logic (for demonstration purposes)
        var matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        var matches = new MatOfDMatch();
        matcher.match(logoDescriptors, inputDescriptor, matches);

        long goodMatches = matches.toList().stream()
                .filter(m -> m.distance < 50) // Threshold for good matches
                .count();

        // Clean up
        inputImage.release();
        inputDescriptor.release();
        inputKeypoints.release();

        return goodMatches >= 10; // Arbitrary threshold for logo detection
    }

    public Blob downloadFile(String uuid) {
        Blob blob = storage.get(BlobId.of(bucketName, uuid));
        return blob;
    }

    @PreDestroy
    void cleanup() {
        logoDescriptors.release();
        logoKeypoints.release();
    }
}
