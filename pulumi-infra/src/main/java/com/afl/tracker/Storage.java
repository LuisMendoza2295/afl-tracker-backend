package com.afl.tracker;

import com.pulumi.Context;
import com.pulumi.gcp.serviceaccount.Account;
import com.pulumi.gcp.storage.Bucket;
import com.pulumi.gcp.storage.BucketArgs;
import com.pulumi.gcp.storage.BucketIAMMember;
import com.pulumi.gcp.storage.BucketIAMMemberArgs;

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
        .publicAccessPrevention("inherited")
        .build());

    new BucketIAMMember(bucketName, BucketIAMMemberArgs.builder()
        .bucket(storageBucket.name())
        .role("roles/storage.objectViewer")
        .member("allUsers")
        .build());

    return storageBucket;
  }
}
