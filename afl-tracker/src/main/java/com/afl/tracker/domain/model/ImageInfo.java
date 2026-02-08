package com.afl.tracker.domain.model;

import java.time.Instant;

import com.afl.tracker.domain.model.valueobj.Location;

public record ImageInfo(
        String id,
        String url,
        Location location,
        UserInfo uploadedBy,
        Instant uploadedAt,
        String fileName,
        Long fileSize) {

  public ImageInfo withId(String id) {
    return new ImageInfo(id, url, location, uploadedBy, uploadedAt, fileName, fileSize);
  }
}
