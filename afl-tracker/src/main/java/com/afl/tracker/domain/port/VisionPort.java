package com.afl.tracker.domain.port;

import java.nio.file.Path;

import com.afl.tracker.domain.model.VisionInfo;

public interface VisionPort {

  VisionInfo getVisionInfo(Path imagePath);
}
