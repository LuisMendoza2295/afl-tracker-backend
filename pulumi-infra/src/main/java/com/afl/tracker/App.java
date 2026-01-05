package com.afl.tracker;

import com.pulumi.Pulumi;
import com.pulumi.gcp.artifactregistry.Repository;
import com.pulumi.gcp.artifactregistry.RepositoryArgs;

public class App {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            var artifactRepository = new Repository("afl-tracker-repo", RepositoryArgs.builder()
                    .location("us-east1")
                    .repositoryId("afl-tracker-repo")
                    .format("DOCKER")
                    .build());
            
            ctx.export("registry-uri", artifactRepository.registryUri());
        });
    }
}
