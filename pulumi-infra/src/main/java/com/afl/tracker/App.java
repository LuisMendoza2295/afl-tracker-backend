package com.afl.tracker;

import com.afl.tracker.gcp.CloudRun;
import com.afl.tracker.gcp.Storage;
import com.afl.tracker.azure.CustomVision;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.StackReference;
import com.pulumi.resources.StackReferenceArgs;

public class App {
  public static void main(String[] args) {
    Pulumi.run(ctx -> {
      String projectId = ctx.config("gcp").require("project");
      String region = ctx.config("gcp").require("region");
      String infraStackName = ctx.config().require("infra-stack");

      // Reference platform infrastructure stack
      var platformStack = new StackReference(infraStackName + "/dev");
      
      
      // Get GCP platform outputs using .output() not .requireOutput()
      var gcpRuntimeSAEmail = platformStack.output("gcpBackendRuntimeSAEmail").applyValue(v -> v.toString());
      var gcpArtifactRegistryName = platformStack.output("gcpArtifactRegistryName").applyValue(v -> v.toString());
      var gcpVpcName = platformStack.output("gcpVpcName").applyValue(v -> v.toString());
      var gcpPrivateSubnetName = platformStack.output("gcpPrivateSubnetName").applyValue(v -> v.toString());
      
      // Export GCP platform info
      ctx.export("RUNTIME_SA_EMAIL", gcpRuntimeSAEmail);
      var gcpRepositoryUrl = Output.format("%s-docker.pkg.dev/%s/%s", region, projectId, gcpArtifactRegistryName);
      ctx.export("REPOSITORY_URL", gcpRepositoryUrl);

      // ===== GCP Resources =====
      
      // Enable Firestore and grant access to backend SA
      com.afl.tracker.gcp.Firestore.enableAndGrantAccess(ctx, gcpRuntimeSAEmail);
      
      // Create Storage Bucket
      var storageBucket = Storage.create(ctx, gcpRuntimeSAEmail);
      ctx.export("STORAGE_BUCKET_URL", storageBucket.url());
      ctx.export("STORAGE_BUCKET_NAME", storageBucket.name());

      // ===== Azure Resources =====
      
      // Create Custom Vision (Training + Prediction)
      var customVision = new CustomVision(ctx, platformStack);
      ctx.export("AZURE_CV_TRAINING_ENDPOINT", customVision.getTrainingEndpoint());
      ctx.export("AZURE_CV_PREDICTION_ENDPOINT", customVision.getPredictionEndpoint());
      ctx.export("AZURE_CV_TRAINING_KEY", customVision.getTrainingKey());
      ctx.export("AZURE_CV_PREDICTION_KEY", customVision.getPredictionKey());

      // Create Cloud Run Service with Custom Vision configuration
      // Read app-image from config (set by PULUMI_CONFIG_PASSTHROUGH in CI/CD), fallback to :latest
      var latestImage = Output.format("%s/afl-tracker-backend:latest", gcpRepositoryUrl);
      var appImage = ctx.config().get("app-image")
          .map(Output::of)
          .orElse(latestImage);
      ctx.export("APP_IMAGE", appImage);
      
      // CloudRun handles Custom Vision config (env vars or Pulumi resource)
      var cloudRunService = CloudRun.create(
          ctx,
          appImage,
          gcpRuntimeSAEmail,
          gcpVpcName,
          gcpPrivateSubnetName,
          storageBucket.name(),
          customVision);
      
      ctx.export("CLOUD_RUN_URL", cloudRunService.uri());
    });
  }
}
