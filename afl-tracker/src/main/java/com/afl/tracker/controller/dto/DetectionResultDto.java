package com.afl.tracker.controller.dto;

import java.util.UUID;

import com.afl.tracker.domain.model.VisionInfo;

/**
 * API response DTO for logo detection results
 * Combines detection results with request metadata
 */
public record DetectionResultDto(
        UUID uuid,
        String fileName,
        double confidence,
        boolean isLogo) {

    public static DetectionResultDto from(VisionInfo visionInfo, String fileName) {
        return new DetectionResultDto(
                visionInfo.uuid(),
                fileName,
                visionInfo.confidence(),
                visionInfo.isLogo());
    }
}
