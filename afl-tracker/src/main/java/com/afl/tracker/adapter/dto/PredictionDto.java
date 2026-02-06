package com.afl.tracker.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PredictionDto(
        double probability,
        String tagId,
        String tagName,
        BoundingBoxDto boundingBox) {
}
