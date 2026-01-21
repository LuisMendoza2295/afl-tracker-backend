package com.afl.tracker.vision;

import com.pulumi.Context;
import com.pulumi.gcp.storage.Bucket;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;

public class Vision {

  public static CustomResource setup(Context ctx, Bucket storageBucket) {
    String region = "us-east1";
    String projectId = ctx.config("gcp").require("project");

    var setArgs = new VisionResourceArgs();
    setArgs.project = projectId;
    setArgs.location = region;
    setArgs.productSetId = "afl-logo-set";
    setArgs.displayName = "AFL Logo Set";
    var logoSetResource = new CustomResource(
      "google-native:vision/v1:ProductSet",
      "afl-logo-set",
      setArgs,
      CustomResourceOptions.builder().build());
    assert logoSetResource != null;

    var productArgs = new VisionResourceArgs();
    productArgs.project = projectId;
    productArgs.location = region;
    productArgs.productId = "afl-logo-id";
    productArgs.productCategory = "general-v1";
    productArgs.displayName = "AFL Logo";
    var productResource = new CustomResource(
      "google-native:vision/v1:Product",
      "afl-logo-product",
      productArgs,
      CustomResourceOptions.builder().build());

    var imageArgs = new VisionResourceArgs();
    imageArgs.project = projectId;
    imageArgs.location = region;
    imageArgs.productId = "afl-logo-id";
    imageArgs.referenceImageId = "logo-ref-image-id";
    imageArgs.uri = storageBucket.url().applyValue(url -> url + "/afl-logo.jpg");
    var imageResource = new CustomResource(
      "google-native:vision/v1:ReferenceImage",
      "afl-logo-ref-image",
      imageArgs,
      CustomResourceOptions.builder()
        .dependsOn(productResource)
        .build());

    return imageResource;
  }
}
