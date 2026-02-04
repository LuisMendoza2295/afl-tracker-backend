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
      
      
      // Get GCP platform outputs (cast to String)
      Output<String> gcpRuntimeSAEmail = platformStack.requireOutput("gcpBackendRuntimeSAEmail").applyValue(v -> (String) v);
      Output<String> gcpArtifactRegistryName = platformStack.requireOutput("gcpArtifactRegistryName").applyValue(v -> (String) v);
      Output<String> gcpVpcName = platformStack.requireOutput("gcpVpcName").applyValue(v -> (String) v);
      Output<String> gcpPrivateSubnetName = platformStack.requireOutput("gcpPrivateSubnetName").applyValue(v -> (String) v);
      
      // Export GCP platform info
      ctx.export("RUNTIME_SA_EMAIL", gcpRuntimeSAEmail);
      var repositoryUrl = Output.format("%s-docker.pkg.dev/%s/%s", region, projectId, gcpArtifactRegistryName);
      ctx.export("REPOSITORY_URL", repositoryUrl);

      // ===== GCP Resources =====
      
      // Create Storage Bucket
      var storageBucket = Storage.create(ctx, gcpRuntimeSAEmail);
      ctx.export("STORAGE_BUCKET_URL", storageBucket.url());
      ctx.export("STORAGE_BUCKET_NAME", storageBucket.name());

      // Create Cloud Run Service
      var latestImage = Output.format("%s/afl-tracker-backend:latest", repositoryUrl);
      var appImage = ctx.config().get("app-image").map(Output::of).orElse(latestImage);
      ctx.export("APP_IMAGE", appImage);
      var cloudRunService = CloudRun.create(ctx, appImage, gcpRuntimeSAEmail, gcpVpcName, gcpPrivateSubnetName);
      ctx.export("CLOUD_RUN_URL", cloudRunService.uri());

      // ===== Azure Resources =====
      
      // Create Custom Vision (Training + Prediction)
      var customVision = new CustomVision(ctx, platformStack);
      ctx.export("AZURE_CV_TRAINING_ENDPOINT", customVision.getTrainingEndpoint());
      ctx.export("AZURE_CV_PREDICTION_ENDPOINT", customVision.getPredictionEndpoint());
      ctx.export("AZURE_CV_TRAINING_KEY", customVision.getTrainingKey());
      ctx.export("AZURE_CV_PREDICTION_KEY", customVision.getPredictionKey());
    });
  }
}
