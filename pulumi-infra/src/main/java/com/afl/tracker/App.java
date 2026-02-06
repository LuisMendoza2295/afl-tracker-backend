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

      // Use prediction key from Azure (already marked as secret)
      Output<String> cvPredictionKeySecret = customVision.getPredictionKey();

      // ===== Cloud Run Service =====
      
      // Create Cloud Run Service with Custom Vision configuration
      // Read app-image from config (set dynamically by CI/CD via PULUMI_CONFIG), fallback to :latest for local dev
      String appImageFromEnv = getPulumiConfigValue("afl-tracker-backend:app-image");
      Output<String> appImage;
      if (appImageFromEnv != null && !appImageFromEnv.isEmpty()) {
        System.out.println("[DEBUG] Using app-image from PULUMI_CONFIG: " + appImageFromEnv);
        appImage = Output.of(appImageFromEnv);
      } else {
        String appImageFromConfig = ctx.config().get("app-image").orElse(null);
        if (appImageFromConfig != null && !appImageFromConfig.isEmpty()) {
          System.out.println("[DEBUG] Using app-image from Pulumi config: " + appImageFromConfig);
          appImage = Output.of(appImageFromConfig);
        } else {
          System.out.println("[DEBUG] Using default app-image: latest");
          appImage = Output.format("%s/afl-tracker-backend:latest", gcpRepositoryUrl);
        }
      }
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
  
  /**
   * Parse PULUMI_CONFIG environment variable to get config value.
   * PULUMI_CONFIG is a JSON object like: {"key": "value", ...}
   */
  private static String getPulumiConfigValue(String key) {
    String pulumiConfig = System.getenv("PULUMI_CONFIG");
    if (pulumiConfig == null || pulumiConfig.isEmpty()) {
      return null;
    }
    
    // Simple JSON parsing without external dependencies
    // Format: {"key1": "value1", "key2": "value2"}
    String searchKey = "\"" + key + "\":";
    int keyIndex = pulumiConfig.indexOf(searchKey);
    if (keyIndex == -1) {
      return null;
    }
    
    int valueStart = pulumiConfig.indexOf("\"", keyIndex + searchKey.length());
    if (valueStart == -1) {
      return null;
    }
    
    int valueEnd = pulumiConfig.indexOf("\"", valueStart + 1);
    if (valueEnd == -1) {
      return null;
    }
    
    return pulumiConfig.substring(valueStart + 1, valueEnd);
  }
}
