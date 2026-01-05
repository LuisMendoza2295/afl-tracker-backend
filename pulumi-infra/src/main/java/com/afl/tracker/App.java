package com.afl.tracker;

import java.util.Map;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.gcp.artifactregistry.Repository;
import com.pulumi.gcp.artifactregistry.RepositoryArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateContainerPortsArgs;
import com.pulumi.gcp.iam.WorkloadIdentityPool;
import com.pulumi.gcp.iam.WorkloadIdentityPoolArgs;
import com.pulumi.gcp.iam.WorkloadIdentityPoolProvider;
import com.pulumi.gcp.iam.WorkloadIdentityPoolProviderArgs;
import com.pulumi.gcp.iam.inputs.WorkloadIdentityPoolProviderOidcArgs;
import com.pulumi.gcp.serviceaccount.Account;
import com.pulumi.gcp.serviceaccount.AccountArgs;
import com.pulumi.gcp.serviceaccount.IAMMember;
import com.pulumi.gcp.serviceaccount.IAMMemberArgs;

public class App {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {

            String projectId = ctx.config("gcp").require("project");
            String region = ctx.config("gcp").get("region").orElse("us-east1");

            String githubRepo = "LuisMendoza2295/afl-tracker-backend";

            // 1. Create an Artifact Repository
            String artifactoryRepoName = "afl-tracker-repo";
            var artifactRepository = new Repository(artifactoryRepoName, RepositoryArgs.builder()
                    .location("us-east1")
                    .repositoryId(artifactoryRepoName)
                    .description("Artifact repository for Images")
                    .format("DOCKER")
                    .build());

            // 2. Create Deployment Service Account
            String deployServiceAccountName = "github-deploy-sa";
            var deployServiceAccount = new Account(deployServiceAccountName, AccountArgs.builder()
                    .accountId(deployServiceAccountName)
                    .displayName("Service Account for deploying AFL Tracker Application")
                    .build());

            // 3. Grant Service Account Permissions
            String artifactRegistryWriterName = "sa-registry-writer";
            var artifactRegistryWriter = new com.pulumi.gcp.projects.IAMMember(artifactRegistryWriterName, com.pulumi.gcp.projects.IAMMemberArgs.builder()
                    .project(projectId)
                    .role("roles/artifactregistry.writer")
                    .member(deployServiceAccount.email().applyValue(email -> "serviceAccount:" + email))
                    .build());

            // 4. Workload Identity Pool
            String wifPoolName = "wif-github-pool";
            var githubPool = new WorkloadIdentityPool(wifPoolName, WorkloadIdentityPoolArgs.builder()
                    .workloadIdentityPoolId(wifPoolName)
                    .displayName("WIF Pool for GitHub Actions")
                    .description("Workload Identity Pool to allow GitHub Actions to impersonate GCP Service Account")
                    .build());

            // 5. Workload Identity Provider
            String githubProviderName = "wif-github-provider";
            var githubProvider = new WorkloadIdentityPoolProvider(githubProviderName, WorkloadIdentityPoolProviderArgs.builder()
                    .workloadIdentityPoolId(githubPool.workloadIdentityPoolId())
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

            // 6. Service Account IAM role for token impersonation
            Output<String> memberString = githubPool.name().applyValue(poolId -> String.format(
                    "principalSet://iam.googleapis.com/%s/attribute.repository/%s",
                    poolId,
                    githubRepo));
            var saIamMember = new IAMMember("wif-sa-token-creator", IAMMemberArgs.builder()
                    .serviceAccountId(deployServiceAccount.name())
                    .role("roles/iam.serviceAccountTokenCreator")
                    .member(memberString)
                    .build());
            
            // Export relevant info
            ctx.export("WIF_PROVIDER", githubProvider.name());
            ctx.export("WIF_SERVICE_ACCOUNT", deployServiceAccount.email());
            ctx.export("REPOSITORY_URL", artifactRepository.name().applyValue(name -> String.format("%s-docker.pkg.dev/%s/%s", region, projectId, artifactoryRepoName)));

            // 7. Grant Cloud Run Developer Role to Service Account
            String cloudRunDeployerName = "sa-cloud-run-deployer";
            var cloudRunDeployer = new com.pulumi.gcp.projects.IAMMember(cloudRunDeployerName, com.pulumi.gcp.projects.IAMMemberArgs.builder()
                    .project(projectId)
                    .role("roles/run.developer")
                    .member(deployServiceAccount.email().applyValue(email -> "serviceAccount:" + email))
                    .build());
                
            // 8. Grant Service Account User Role to Service Account
            String serviceAccountDeployUserName = "sa-deploy-user";
            var serviceAccountDeployUser = new com.pulumi.gcp.projects.IAMMember(serviceAccountDeployUserName, com.pulumi.gcp.projects.IAMMemberArgs.builder()
                    .project(projectId)
                    .role("roles/iam.serviceAccountUser")
                    .member(deployServiceAccount.email().applyValue(email -> "serviceAccount:" + email))
                    .build());

            // 9. Define Cloud Run v2 Service
            String cloudRunServiceName = "cloud-run-v2-service";
            Output<String> latestImage = artifactRepository.registryUri().applyValue(uri -> uri + "/afl-tracker-backend:latest");
            Output<String> appImage = ctx.config().get("app-image").map(Output::of).orElse(latestImage);
            var cloudRunService = new com.pulumi.gcp.cloudrunv2.Service(cloudRunServiceName, com.pulumi.gcp.cloudrunv2.ServiceArgs.builder()
                    .name("afl-tracker-backend-service")
                    .location(region)
                    .ingress("INGRESS_TRAFFIC_ALL")
                    .template(com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateArgs.builder()
                            .containers(com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateContainerArgs.builder()
                                    .image(appImage)
                                    .ports(ServiceTemplateContainerPortsArgs.builder()
                                        .containerPort(8080)
                                        .build())
                                    .build())
                            .build())
                    .build());
            ctx.export("CLOUD_RUN_URL", cloudRunService.uri());
        });
    }
}
