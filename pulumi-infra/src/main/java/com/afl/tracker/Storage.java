package com.afl.tracker;

import com.pulumi.Context;
import com.pulumi.gcp.serviceaccount.Account;
import com.pulumi.gcp.storage.Bucket;
import com.pulumi.gcp.storage.BucketArgs;

public class Storage {

  public static Bucket createStorageBucket(Context ctx, Account deploySA) {
    String bucketName = ctx.config().require("storageBucketName");
    String projectId = ctx.config("gcp").require("project");
    var storageBucket = new Bucket(bucketName, BucketArgs.builder()
        .name(bucketName)
        .project(projectId)
        .storageClass("STANDARD")
        .location("US")
        .uniformBucketLevelAccess(true)
        .build());

    return storageBucket;
  }
}
