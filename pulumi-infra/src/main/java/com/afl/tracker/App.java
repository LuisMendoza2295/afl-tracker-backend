package com.afl.tracker;

import static com.pulumi.gcp.serviceaccount.Account.get;
import static com.afl.tracker.CloudRun.createCloudRunService;
import static com.afl.tracker.Storage.createStorageBucket;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.gcp.serviceaccount.Account;
import com.pulumi.gcp.serviceaccount.AccountArgs;

public class App {
  public static void main(String[] args) {
    Pulumi.run(ctx -> {
      String projectId = ctx.config("gcp").require("project");
      String region = ctx.config("gcp").require("region");
      String artifactoryRepoName = ctx.config().require("artifactoryRepoName");

      var runtimeSA = new Account("afl-backend-runtime-sa", AccountArgs.builder()
        .accountId("afl-backend-runtime")
        .displayName("Runtime identity for AFL Quarkus Backend")
        .build());
      ctx.export("RUNTIME_WIF_SERVICE_ACCOUNT", runtimeSA.email());

      // Export WIF info
      var repositoryUrl = String.format("%s-docker.pkg.dev/%s/%s", region, projectId, artifactoryRepoName);
      ctx.export("REPOSITORY_URL", repositoryUrl);

      // Create Storage Bucket
      var storageBucket = createStorageBucket(ctx, runtimeSA);
      ctx.export("STORAGE_BUCKET_URL", storageBucket.url());
      ctx.export("STORAGE_BUCKET_NAME", storageBucket.name());

      // Define Cloud Run v2 Service
      String latestImage = String.format("%s/afl-tracker-backend:latest", repositoryUrl);
      Output<String> appImage = ctx.config().get("app-image").map(Output::of).orElse(Output.of(latestImage));
      var cloudRunService = createCloudRunService(ctx, appImage, runtimeSA);

      ctx.export("CLOUD_RUN_URL", cloudRunService.uri());
    });
  }
}
