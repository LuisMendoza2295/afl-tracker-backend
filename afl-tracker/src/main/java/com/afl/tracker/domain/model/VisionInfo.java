package com.afl.tracker.domain.model;

import java.util.UUID;

public record VisionInfo(
        UUID uuid,
        double confidence) {

    public static final double CONFIDENCE_THRESHOLD = 0.70;

    public boolean isLogo() {
      return confidence >= CONFIDENCE_THRESHOLD;
    }

    public static VisionInfo empty() {
        return new VisionInfo(UUID.randomUUID(), 0.0);
    }
}
