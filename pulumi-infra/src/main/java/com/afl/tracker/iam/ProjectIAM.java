package com.afl.tracker.iam;

import com.pulumi.Context;
import com.pulumi.gcp.projects.IAMMember;
import com.pulumi.gcp.projects.IAMMemberArgs;
import com.pulumi.gcp.serviceaccount.Account;

public class ProjectIAM {

  public static IAMMember grantRoleToSA(Context ctx, String name, String role,
      Account deploySA) {
    var iamMember = new IAMMember(name, IAMMemberArgs.builder()
        .project(ctx.config("gcp").require("project"))
        .role(role)
        .member(deploySA.email().applyValue(email -> "serviceAccount:" + email))
        .build());
    return iamMember;
  }
}
