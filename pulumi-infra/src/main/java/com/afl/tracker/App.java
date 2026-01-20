package com.afl.tracker;

import static com.afl.tracker.CloudRun.createCloudRunService;
import static com.afl.tracker.Storage.createStorageBucket;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.StackReference;

public class App {
  public static void main(String[] args) {
    Pulumi.run(ctx -> {
      String projectId = ctx.config("gcp").require("project");
      String region = ctx.config("gcp").require("region");
      String artifactoryRepoName = ctx.config().require("artifactoryRepoName");
      String infraStackName = ctx.config().require("infra-stack");

      var infraStack = new StackReference(infraStackName + "/dev");
      var runtimeSAEmail = infraStack.output("backendRuntimeSAEmail").applyValue(v -> v.toString());
      ctx.export("RUNTIME_SA_EMAIL", runtimeSAEmail);

      // Export WIF info
      var repositoryUrl = String.format("%s-docker.pkg.dev/%s/%s", region, projectId, artifactoryRepoName);
      ctx.export("REPOSITORY_URL", repositoryUrl);

      // Create Storage Bucket
      var storageBucket = createStorageBucket(ctx, runtimeSAEmail);
      ctx.export("STORAGE_BUCKET_URL", storageBucket.url());
      ctx.export("STORAGE_BUCKET_NAME", storageBucket.name());

      // Define Cloud Run v2 Service
      String latestImage = String.format("%s/afl-tracker-backend:latest", repositoryUrl);
      Output<String> appImage = ctx.config().get("app-image").map(Output::of).orElse(Output.of(latestImage));
      var cloudRunService = createCloudRunService(ctx, appImage, runtimeSAEmail);

      ctx.export("CLOUD_RUN_URL", cloudRunService.uri());
    });
  }
}
