package com.afl.tracker.gcp;

import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.gcp.projects.IAMMember;
import com.pulumi.gcp.projects.IAMMemberArgs;
import com.pulumi.gcp.projects.Service;
import com.pulumi.gcp.projects.ServiceArgs;
import com.pulumi.resources.CustomResourceOptions;

public class Firestore {

  public static void enableAndGrantAccess(
      Context ctx,
      Output<String> runtimeSAEmail) {

    String projectId = ctx.config("gcp").require("project");

    // Enable Firestore API
    var firestoreApi = new Service("firestore-api",
        ServiceArgs.builder()
            .service("firestore.googleapis.com")
            .disableOnDestroy(false)
            .build());

    // Grant Firestore access to backend runtime SA
    new IAMMember("backend-firestore-access",
        IAMMemberArgs.builder()
            .project(projectId)
            .role("roles/datastore.user")
            .member(runtimeSAEmail.applyValue(email -> String.format("serviceAccount:%s", email)))
            .build(),
        CustomResourceOptions.builder()
            .dependsOn(firestoreApi)
            .build());
  }
}
