package com.afl.tracker;

import static com.afl.tracker.CloudRun.createCloudRunService;
import static com.afl.tracker.Storage.createStorageBucket;
import static com.afl.tracker.iam.BucketIAM.grantBucketRoleToSA;
import static com.afl.tracker.iam.DeployWIF.createSAImpersonationForPool;
import static com.afl.tracker.iam.DeployWIF.createWorkloadIdentityPool;
import static com.afl.tracker.iam.DeployWIF.createWorkloadIdentityProvider;
import static com.afl.tracker.iam.ProjectIAM.grantRoleToSA;

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

      String githubRepo = "LuisMendoza2295/afl-tracker-backend";

      // Create Service Account for Deployment
      var deploySA = createServiceAccountForDeploy();
      String setIamPolicyRole = "roles/run.services.setIamPolicy";
      grantRoleToSA(ctx, "set-iam-policy", setIamPolicyRole, deploySA);

      var artifactRepository = ArtifactRepository.createArtifactRepository(ctx, deploySA);
      String artifactRegistryWriterName = "sa-registry-writer";
      String role = "roles/artifactregistry.writer";
      grantRoleToSA(ctx, artifactRegistryWriterName, role, deploySA);

      // Workload Identity Federation Setup
      var githubPool = createWorkloadIdentityPool();
      var githubProvider = createWorkloadIdentityProvider(githubPool, githubRepo);

      // Service Account IAM role for token impersonation
      createSAImpersonationForPool(githubPool, githubRepo, deploySA);

      // Export WIF info
      ctx.export("WIF_PROVIDER", githubProvider.name());
      ctx.export("WIF_SERVICE_ACCOUNT", deploySA.email());
      ctx.export("REPOSITORY_URL", artifactRepository.name()
          .applyValue(name -> String.format("%s-docker.pkg.dev/%s/%s", region, projectId, artifactoryRepoName)));

      // Create Storage Bucket
      String storageAdminRole = "roles/storage.admin";
      grantRoleToSA(ctx, "storage-bucket-admin", storageAdminRole, deploySA);
      var storageBucket = createStorageBucket(ctx, deploySA);
      grantBucketRoleToSA(ctx, deploySA);
      ctx.export("BUCKET_NAME", storageBucket.name());

      // Grant additional roles to the deploy SA for Cloud Run deployment
      String cloudRunDeployerName = "sa-cloud-run-deployer";
      String cloudRunDeployRole = "roles/run.developer";
      grantRoleToSA(ctx, cloudRunDeployerName, cloudRunDeployRole, deploySA);
      String serviceAccountDeployUserName = "sa-deploy-user";
      String saUserDeployRole = "roles/iam.serviceAccountUser";
      grantRoleToSA(ctx, serviceAccountDeployUserName, saUserDeployRole, deploySA);

      // Define Cloud Run v2 Service
      Output<String> latestImage = artifactRepository.registryUri()
          .applyValue(uri -> uri + "/afl-tracker-backend:latest");
      Output<String> appImage = ctx.config().get("app-image").map(Output::of).orElse(latestImage);
      var cloudRunService = createCloudRunService(ctx, appImage, deploySA);

      ctx.export("CLOUD_RUN_URL", cloudRunService.uri());
    });
  }

  public static Account createServiceAccountForDeploy() {
    String deployServiceAccountName = "github-deploy-sa";
    var deployServiceAccount = new Account(deployServiceAccountName, AccountArgs.builder()
        .accountId(deployServiceAccountName)
        .displayName("Service Account for deploying AFL Tracker Application")
        .build());
    return deployServiceAccount;
  }
}
