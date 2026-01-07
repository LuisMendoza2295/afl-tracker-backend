package com.afl.tracker;

import java.util.Map;

import com.pulumi.core.Output;
import com.pulumi.gcp.iam.WorkloadIdentityPool;
import com.pulumi.gcp.iam.WorkloadIdentityPoolArgs;
import com.pulumi.gcp.iam.WorkloadIdentityPoolProvider;
import com.pulumi.gcp.iam.WorkloadIdentityPoolProviderArgs;
import com.pulumi.gcp.iam.inputs.WorkloadIdentityPoolProviderOidcArgs;
import com.pulumi.gcp.serviceaccount.Account;
import com.pulumi.gcp.serviceaccount.IAMMember;
import com.pulumi.gcp.serviceaccount.IAMMemberArgs;

public class DeployWIF {

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

  public static IAMMember createSAImpersonationForPool(WorkloadIdentityPool pool, String githubRepo, Account deploySA) {
    Output<String> memberString = pool.name().applyValue(poolId -> String.format(
        "principalSet://iam.googleapis.com/%s/attribute.repository/%s",
        poolId,
        githubRepo));
    var saIamMember = new IAMMember("wif-sa-token-creator",
        IAMMemberArgs.builder()
            .serviceAccountId(deploySA.name())
            .role("roles/iam.serviceAccountTokenCreator")
            .member(memberString)
            .build());
    return saIamMember;
  }
}
