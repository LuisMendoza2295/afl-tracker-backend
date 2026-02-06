package com.afl.tracker.adapter.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomVisionResponseDto(
        String id,
        String project,
        String iteration,
        List<PredictionDto> predictions) {
}
