package com.afl.tracker.adapter.datastore.mapper;

import java.time.Instant;

import com.afl.tracker.adapter.datastore.dto.ImageMetadataDto;
import com.afl.tracker.adapter.datastore.dto.LocationDto;
import com.afl.tracker.adapter.datastore.dto.UserInfoDto;
import com.afl.tracker.domain.model.ImageInfo;
import com.afl.tracker.domain.model.UserInfo;
import com.afl.tracker.domain.model.type.UserOrigin;
import com.afl.tracker.domain.model.valueobj.Location;
import com.google.cloud.Timestamp;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ImageInfoMapper {

  public ImageMetadataDto toImageMetadata(ImageInfo imageInfo) {
    ImageMetadataDto metadataDto = new ImageMetadataDto();
    metadataDto.setId(imageInfo.id());
    metadataDto.setUrl(imageInfo.url());
    metadataDto.setFileName(imageInfo.fileName());
    metadataDto.setFileSize(imageInfo.fileSize());
    metadataDto.setUploadedAt(Timestamp.ofTimeSecondsAndNanos(imageInfo.uploadedAt().getEpochSecond(), imageInfo.uploadedAt().getNano()));
    metadataDto.setUploadedBy(toUserInfoDto(imageInfo.uploadedBy()));
    metadataDto.setLocation(toLocationDto(imageInfo.location()));
    return metadataDto;
  }

  public LocationDto toLocationDto(Location location) {
    return new LocationDto(location.latitude(), location.longitude());
  }

  public UserInfoDto toUserInfoDto(UserInfo userInfo) {
    return new UserInfoDto(userInfo.id(), userInfo.name(), userInfo.email(), userInfo.photoUrl(), userInfo.origin().name());
  }

  public ImageInfo toImageInfo(ImageMetadataDto metadataDto) {
    return new ImageInfo(
      metadataDto.getId(),
      metadataDto.getUrl(),
      toLocation(metadataDto.getLocation()),
      toUserInfo(metadataDto.getUploadedBy()),
      Instant.ofEpochSecond(metadataDto.getUploadedAt().getSeconds(), metadataDto.getUploadedAt().getNanos()),
      metadataDto.getFileName(),
      metadataDto.getFileSize());
  }

  public UserInfo toUserInfo(UserInfoDto userInfoDto) {
    return new UserInfo(userInfoDto.id(), userInfoDto.name(), userInfoDto.email(), userInfoDto.photoUrl(), UserOrigin.valueOf(userInfoDto.origin()));
  }

  public Location toLocation(LocationDto locationDto) {
    return new Location(locationDto.latitude(), locationDto.longitude());
  }
}
