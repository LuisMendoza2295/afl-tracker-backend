package com.afl.tracker;

import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.gcp.storage.Bucket;
import com.pulumi.gcp.storage.BucketArgs;
import com.pulumi.gcp.storage.BucketIAMMember;
import com.pulumi.gcp.storage.BucketIAMMemberArgs;
import com.pulumi.gcp.storage.inputs.BucketCorArgs;

public class Storage {

  public static Bucket createStorageBucket(Context ctx, Output<String> runtimeSAEmail) {
    String bucketName = ctx.config().require("storageBucketName");
    String projectId = ctx.config("gcp").require("project");
    String region = ctx.config("gcp").require("region");
    var storageBucket = new Bucket(bucketName, BucketArgs.builder()
        .name(bucketName)
        .project(projectId)
        .cors(BucketCorArgs.builder()
          .origins("*")
          .methods("GET", "HEAD", "PUT", "POST", "DELETE")
          .responseHeaders("Content-Type")
          .maxAgeSeconds(3600)
          .build())
        .storageClass("STANDARD")
        .location(region)
        .uniformBucketLevelAccess(true)
        .build());

    new BucketIAMMember("bucket-backend-admin", BucketIAMMemberArgs.builder()
        .bucket(storageBucket.name())
        .role("roles/storage.objectAdmin")
        .member(runtimeSAEmail.applyValue(email -> "serviceAccount:" + email))
        .build());

    new BucketIAMMember("bucket-public-read", BucketIAMMemberArgs.builder()
        .bucket(storageBucket.name())
        .role("roles/storage.objectViewer")
        .member("allUsers")
        .build());

    return storageBucket;
  }
}
