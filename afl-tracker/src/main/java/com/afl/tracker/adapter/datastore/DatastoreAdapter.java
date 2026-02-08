package com.afl.tracker.adapter.datastore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import com.afl.tracker.adapter.datastore.dto.ImageMetadataDto;
import com.afl.tracker.adapter.datastore.mapper.ImageInfoMapper;
import com.afl.tracker.domain.model.ImageInfo;
import com.afl.tracker.domain.port.DatastorePort;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DatastoreAdapter implements DatastorePort {

  private static final String IMAGES_COLLECTION = "images";
    
  @Inject
  Firestore firestore;

  @Inject
  ImageInfoMapper imageInfoMapper;

  @Override
  public ImageInfo saveImage(ImageInfo imageInfo) {
    try {
      ImageMetadataDto imageMetadataDto = imageInfoMapper.toImageMetadata(imageInfo);

      ApiFuture<DocumentReference> future = firestore.collection(IMAGES_COLLECTION)
              .add(imageMetadataDto);
      DocumentReference docRef = future.get();
      return imageInfo.withId(docRef.getId());
    } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException("Failed to save image metadata", e);
    }
  }

  @Override
  public List<ImageInfo> getAllImages() {
    try {
        ApiFuture<QuerySnapshot> future = firestore.collection(IMAGES_COLLECTION).get();
        QuerySnapshot querySnapshot = future.get();
        
        List<ImageMetadataDto> imageMetadataDtos = new ArrayList<>();
        for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
            ImageMetadataDto imageInfo = document.toObject(ImageMetadataDto.class);
            imageMetadataDtos.add(imageInfo);
        }
        
        return imageMetadataDtos.stream()
          .map(imageInfoMapper::toImageInfo)
          .toList();
    } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException("Failed to fetch images", e);
    }
  }

  @Override
  public ImageInfo getImageById(String id) {
    Objects.requireNonNull(id, "ImageInfo id must not be null");
    try {
      ApiFuture<DocumentSnapshot> future = firestore.collection(IMAGES_COLLECTION)
              .document(id)
              .get();
      DocumentSnapshot document = future.get();
      
      if (document.exists()) {
        ImageMetadataDto imageInfo = document.toObject(ImageMetadataDto.class);
        return imageInfoMapper.toImageInfo(imageInfo);
      }
      
      return null;
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Failed to fetch image", e);
    }
  }
}
