package com.afl.tracker.domain.port;

import java.util.List;

import com.afl.tracker.domain.model.ImageInfo;

public interface DatastorePort {

  ImageInfo saveImage(ImageInfo imageInfo);

  List<ImageInfo> getAllImages();

  ImageInfo getImageById(String id);
}
