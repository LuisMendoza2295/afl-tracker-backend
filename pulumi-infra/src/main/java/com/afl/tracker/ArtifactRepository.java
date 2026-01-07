package com.afl.tracker;

import com.pulumi.Context;
import com.pulumi.gcp.artifactregistry.Repository;
import com.pulumi.gcp.artifactregistry.RepositoryArgs;
import com.pulumi.gcp.serviceaccount.Account;

public class ArtifactRepository {

  public static Repository createArtifactRepository(Context ctx, Account deploySA) {
    String artifactoryRepoName = ctx.config().require("artifactoryRepoName");
    String region = ctx.config("gcp").require("region");
    var artifactRepository = new Repository(artifactoryRepoName, RepositoryArgs.builder()
        .location(region)
        .repositoryId(artifactoryRepoName)
        .description("Artifact repository for Images")
        .format("DOCKER")
        .build());
    return artifactRepository;
  }
}
