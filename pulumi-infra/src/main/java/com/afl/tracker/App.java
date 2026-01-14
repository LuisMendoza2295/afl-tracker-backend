package com.afl.tracker;

import static com.pulumi.gcp.serviceaccount.Account.get;
import static com.afl.tracker.ArtifactRepository.createArtifactRepository;
import static com.afl.tracker.CloudRun.createCloudRunService;
import static com.afl.tracker.DeployWIF.createSAImpersonationForPool;
import static com.afl.tracker.DeployWIF.createWorkloadIdentityPool;
import static com.afl.tracker.DeployWIF.createWorkloadIdentityProvider;
import static com.afl.tracker.Storage.createStorageBucket;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;

public class App {
  public static void main(String[] args) {
    Pulumi.run(ctx -> {
      String projectId = ctx.config("gcp").require("project");
      String region = ctx.config("gcp").require("region");
      String artifactoryRepoName = ctx.config().require("artifactoryRepoName");
      Output<String> infraSAEmail = ctx.config().requireSecret("infra-sa-email");

      String githubRepo = "LuisMendoza2295/afl-tracker-backend";

      // Get Service Account for Deployment
      var infraSA = get("existing-infra-sa", infraSAEmail, null, null);

      // Workload Identity Federation Setup
      var githubPool = createWorkloadIdentityPool();
      var githubProvider = createWorkloadIdentityProvider(githubPool, githubRepo);

      // Service Account IAM role for token impersonation
      createSAImpersonationForPool(githubPool, githubRepo, infraSA);

      // Create Artifact Repository
      var artifactRepository = createArtifactRepository(ctx, infraSA);

      // Export WIF info
      ctx.export("WIF_PROVIDER", githubProvider.name());
      ctx.export("WIF_SERVICE_ACCOUNT", infraSA.email());
      ctx.export("REPOSITORY_URL", artifactRepository.name()
          .applyValue(name -> String.format("%s-docker.pkg.dev/%s/%s", region, projectId, artifactoryRepoName)));

      // Create Storage Bucket
      var storageBucket = createStorageBucket(ctx, infraSA);
      ctx.export("BUCKET_NAME", storageBucket.name());

      // Define Cloud Run v2 Service
      Output<String> latestImage = artifactRepository.registryUri()
          .applyValue(uri -> uri + "/afl-tracker-backend:latest");
      Output<String> appImage = ctx.config().get("app-image").map(Output::of).orElse(latestImage);
      var cloudRunService = createCloudRunService(ctx, appImage, infraSA);

      ctx.export("CLOUD_RUN_URL", cloudRunService.uri());
    });
  }
}
