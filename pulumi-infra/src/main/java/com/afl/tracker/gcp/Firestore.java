package com.afl.tracker.gcp;

import com.pulumi.Context;
import com.pulumi.gcp.firestore.Database;
import com.pulumi.gcp.firestore.DatabaseArgs;

public class Firestore {

    public static Database create(Context ctx, String databaseName) {
        String projectId = ctx.config("gcp").require("project");
        String region = ctx.config("gcp").require("region");

        // Create the Firestore Native database with the specified name
        // Note: The Firestore API must be enabled in the project (handled by infra or manually)
        return new Database(databaseName,
            DatabaseArgs.builder()
                .project(projectId)
                .locationId(region)
                .type("FIRESTORE_NATIVE")
                .build());
    }
}
