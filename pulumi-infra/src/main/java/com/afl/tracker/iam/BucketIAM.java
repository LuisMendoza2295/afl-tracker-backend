package com.afl.tracker.iam;

import com.pulumi.Context;
import com.pulumi.gcp.serviceaccount.Account;
import com.pulumi.gcp.storage.BucketIAMMember;
import com.pulumi.gcp.storage.BucketIAMMemberArgs;

public class BucketIAM {

  public static BucketIAMMember grantBucketRoleToSA(Context ctx, Account deploySA) {
    String bucketName = ctx.config().require("storageBucketName");
    var bucketIamMember = new BucketIAMMember(bucketName + "-iam-member",
        BucketIAMMemberArgs.builder()
            .bucket(bucketName)
            .role("roles/storage.admin")
            .member(deploySA.email().applyValue(email -> "serviceAccount:" + email))
            .build());
    return bucketIamMember;
  }
}
