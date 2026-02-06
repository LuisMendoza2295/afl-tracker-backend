package com.afl.tracker.adapter.mapper;

import java.util.Optional;
import java.util.UUID;

import com.afl.tracker.adapter.dto.CustomVisionResponseDto;
import com.afl.tracker.adapter.dto.PredictionDto;
import com.afl.tracker.domain.model.VisionInfo;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VisionMapper {

    private static final String TARGET_TAG_NAME = "has-logo";

    /**
     * Parse Custom Vision response DTO and extract confidence and detection tag
     *
     * @param response     Custom Vision API response DTO
     * @return VisionInfo with extracted confidence score and detection tag
     */
    public VisionInfo parseResponse(CustomVisionResponseDto response) {
        // Find the highest confidence prediction for the target tag
        Optional<PredictionDto> bestPrediction = response.predictions().stream()
                .filter(p -> TARGET_TAG_NAME.equalsIgnoreCase(p.tagName()))
                .max((p1, p2) -> Double.compare(p1.probability(), p2.probability()));

        return bestPrediction.map(p -> new VisionInfo(
                    UUID.fromString(response.id()),
                    p.probability()))
                .orElse(VisionInfo.empty());
    }
}
