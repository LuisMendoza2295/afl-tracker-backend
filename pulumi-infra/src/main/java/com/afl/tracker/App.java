package com.afl.tracker;

import java.util.Map;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.gcp.artifactregistry.Repository;
import com.pulumi.gcp.artifactregistry.RepositoryArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateContainerPortsArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTrafficArgs;
import com.pulumi.gcp.iam.WorkloadIdentityPool;
import com.pulumi.gcp.iam.WorkloadIdentityPoolArgs;
import com.pulumi.gcp.iam.WorkloadIdentityPoolProvider;
import com.pulumi.gcp.iam.WorkloadIdentityPoolProviderArgs;
import com.pulumi.gcp.iam.inputs.WorkloadIdentityPoolProviderOidcArgs;
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

                var artifactRepository = createArtifactRepository(ctx, deploySA);

                // Workload Identity Federation Setup
                var githubPool = createWorkloadIdentityPool();
                var githubProvider = createWorkloadIdentityProvider(githubPool, githubRepo);

                // Service Account IAM role for token impersonation
                var saImpersonationRole = createSAImpersonationForPool(githubPool, githubRepo, deploySA);

                // Export WIF info
                ctx.export("WIF_PROVIDER", githubProvider.name());
                ctx.export("WIF_SERVICE_ACCOUNT", deploySA.email());
                ctx.export("REPOSITORY_URL", artifactRepository.name().applyValue(name -> String.format("%s-docker.pkg.dev/%s/%s", region, projectId, artifactoryRepoName)));

                // Create Storage Bucket
                String storageAdminRole = "roles/storage.admin";
                createProjectSA(ctx, "storage-bucket-admin", storageAdminRole, deploySA);
                var storageBucket = createStorageBucket(ctx, deploySA);
                ctx.export("BUCKET_NAME", storageBucket.name());

                // Grant additional roles to the deploy SA for Cloud Run deployment
                String cloudRunDeployerName = "sa-cloud-run-deployer";
                String cloudRunDeployRole = "roles/run.developer";
                var cloudRunDeployer = createProjectSA(ctx, cloudRunDeployerName, cloudRunDeployRole, deploySA);
                String serviceAccountDeployUserName = "sa-deploy-user";
                String saUserDeployRole = "roles/iam.serviceAccountUser";
                var serviceAccountDeployUser = createProjectSA(ctx, serviceAccountDeployUserName, saUserDeployRole, deploySA);

                // Define Cloud Run v2 Service
                String cloudRunServiceName = "cloud-run-v2-service";
                Output<String> latestImage = artifactRepository.registryUri().applyValue(uri -> uri + "/afl-tracker-backend:latest");
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

    public static WorkloadIdentityPool createWorkloadIdentityPool() {
        String wifPoolName = "wif-github-pool";
        var githubPool = new WorkloadIdentityPool(wifPoolName, WorkloadIdentityPoolArgs.builder()
                .workloadIdentityPoolId(wifPoolName)
                .displayName("WIF Pool for GitHub Actions")
                .description("Workload Identity Pool to allow GitHub Actions to impersonate GCP Service Account")
                .build());
        return githubPool;
    }

    public static WorkloadIdentityPoolProvider createWorkloadIdentityProvider(WorkloadIdentityPool pool, String githubRepo) {
        String githubProviderName = "wif-github-provider";
        var githubProvider = new WorkloadIdentityPoolProvider(githubProviderName, WorkloadIdentityPoolProviderArgs.builder()
                .workloadIdentityPoolId(pool.workloadIdentityPoolId())
                .workloadIdentityPoolProviderId(githubProviderName)
                .displayName("GitHub Provider")
                .description("Workload Identity Provider for GitHub Actions")
                .oidc(WorkloadIdentityPoolProviderOidcArgs.builder()
                        .issuerUri("https://token.actions.githubusercontent.com")
                        .build())
                .attributeMapping(Map.of(
                        "google.subject", "assertion.sub",
                        "attribute.repository", "assertion.repository"))
                .attributeCondition("attribute.repository == \"" + githubRepo + "\"")
                .build());
        return githubProvider;
    }

    public static Repository createArtifactRepository(Context ctx, Account deploySA) {
        String artifactoryRepoName = ctx.config().require("artifactoryRepoName");
        String region = ctx.config("gcp").require("region");
        var artifactRepository = new Repository(artifactoryRepoName, RepositoryArgs.builder()
                .location(region)
                .repositoryId(artifactoryRepoName)
                .description("Artifact repository for Images")
                .format("DOCKER")
                .build());

        // 3. Grant Service Account Permissions
        String artifactRegistryWriterName = "sa-registry-writer";
        String role = "roles/artifactregistry.writer";
        var artifactRegistryWriter = createProjectSA(ctx, artifactRegistryWriterName, role, deploySA);
        return artifactRepository;
    }

    public static com.pulumi.gcp.projects.IAMMember createProjectSA(Context ctx, String name, String role, Account deploySA) {
        var iamMember = new com.pulumi.gcp.projects.IAMMember(name, com.pulumi.gcp.projects.IAMMemberArgs.builder()
                .project(ctx.config("gcp").require("project"))
                .role(role)
                .member(deploySA.email().applyValue(email -> "serviceAccount:" + email))
                .build());
        return iamMember;
    }

    public static com.pulumi.gcp.serviceaccount.IAMMember createSAImpersonationForPool(WorkloadIdentityPool pool, String githubRepo, Account deploySA) {
        Output<String> memberString = pool.name().applyValue(poolId -> String.format(
                    "principalSet://iam.googleapis.com/%s/attribute.repository/%s",
                    poolId,
                    githubRepo));
        var saIamMember = new com.pulumi.gcp.serviceaccount.IAMMember("wif-sa-token-creator", com.pulumi.gcp.serviceaccount.IAMMemberArgs.builder()
                .serviceAccountId(deploySA.name())
                .role("roles/iam.serviceAccountTokenCreator")
                .member(memberString)
                .build());
        return saIamMember;
    }

    public static com.pulumi.gcp.cloudrunv2.Service createCloudRunService(Context ctx, Output<String> image, Account deploySA) {
        String serviceName = ctx.config().require("cloudRunServiceName");
        String region = ctx.config("gcp").require("region");
        var cloudRunService = new com.pulumi.gcp.cloudrunv2.Service(serviceName, com.pulumi.gcp.cloudrunv2.ServiceArgs.builder()
                .name(serviceName)
                .location(region)
                .ingress("INGRESS_TRAFFIC_ALL")
                .template(com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateArgs.builder()
                        .serviceAccount(deploySA.email())
                        .containers(com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateContainerArgs.builder()
                                .image(image)
                                .ports(ServiceTemplateContainerPortsArgs.builder()
                                .containerPort(8080)
                                .build())
                                .build())
                        .build())
                .traffics(ServiceTrafficArgs.builder()
                        .type("TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST")
                        .percent(100)
                        .build())
                .build());
        return cloudRunService;
    }

    public static com.pulumi.gcp.storage.Bucket createStorageBucket(Context ctx, Account deploySA) {
        String bucketName = ctx.config().require("storageBucketName");
        String projectId = ctx.config("gcp").require("project");
        var storageBucket = new com.pulumi.gcp.storage.Bucket(bucketName, com.pulumi.gcp.storage.BucketArgs.builder()
                .name(bucketName)
                .project(projectId)
                .storageClass("STANDARD")
                .location("US")
                .uniformBucketLevelAccess(true)
                .build());

        var bucketIamMember = new com.pulumi.gcp.storage.BucketIAMMember(bucketName + "-iam-member", com.pulumi.gcp.storage.BucketIAMMemberArgs.builder()
                .bucket(storageBucket.name())
                .role("roles/storage.admin")
                .member(deploySA.email().applyValue(email -> "serviceAccount:" + email))
                .build());
        return storageBucket;
    }
}
