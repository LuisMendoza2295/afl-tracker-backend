package com.afl.tracker.adapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.afl.tracker.adapter.client.CustomVisionClient;
import com.afl.tracker.adapter.dto.CustomVisionResponseDto;
import com.afl.tracker.adapter.mapper.VisionMapper;
import com.afl.tracker.domain.model.VisionInfo;
import com.afl.tracker.domain.port.VisionPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class VisionAdapter implements VisionPort {

    @Inject
    Logger logger;

    @Inject
    VisionMapper visionMapper;

    @RestClient
    CustomVisionClient customVisionClient;

    @ConfigProperty(name = "azure.customvision.prediction.key")
    String predictionKey;

    @ConfigProperty(name = "azure.customvision.project.id")
    String projectId;

    @ConfigProperty(name = "azure.customvision.iteration.name")
    String iterationName;

    @Override
    public VisionInfo getVisionInfo(Path imagePath) {
        try {
            logger.info("Vision analysis requested for: " + imagePath.getFileName());

            // Read image bytes
            byte[] imageData = Files.readAllBytes(imagePath);

            // Call Custom Vision API using MicroProfile REST Client (Object Detection)
            CustomVisionResponseDto response = customVisionClient.detectImage(
                    projectId,
                    iterationName,
                    predictionKey,
                    imageData);

            // Parse response using mapper
            VisionInfo visionInfo = visionMapper.parseResponse(response);

            logger.info(String.format("Logo detection completed. Confidence: %.2f%%",
                    visionInfo.confidence() * 100));

            return visionInfo;

        } catch (IOException e) {
            logger.error("Failed to read image file: " + imagePath, e);
            return VisionInfo.empty();
        } catch (Exception e) {
            logger.error("Custom Vision prediction failed for: " + imagePath, e);
            return VisionInfo.empty();
        }
    }
}
