package com.afl.tracker.iam;

import com.pulumi.gcp.serviceaccount.Account;
import com.pulumi.gcp.storage.Bucket;
import com.pulumi.gcp.storage.BucketIAMMember;
import com.pulumi.gcp.storage.BucketIAMMemberArgs;

public class BucketIAM {

  public static BucketIAMMember grantRoleToSA(Bucket storageBucket, Account deploySA) {
    var bucketIamMemberName = storageBucket.name().applyValue(name -> name + "-iam-member");
    var bucketIamMember = new BucketIAMMember(bucketIamMemberName.toString(),
        BucketIAMMemberArgs.builder()
            .bucket(storageBucket.name())
            .role("roles/storage.admin")
            .member(deploySA.email().applyValue(email -> "serviceAccount:" + email))
            .build());
    return bucketIamMember;
  }
}
