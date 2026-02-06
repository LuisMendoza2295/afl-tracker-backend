package com.afl.tracker.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BoundingBoxDto(
        double left,
        double top,
        double width,
        double height) {
}
