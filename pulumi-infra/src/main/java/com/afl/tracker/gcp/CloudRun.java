package com.afl.tracker.gcp;

import com.afl.tracker.azure.CustomVision;
import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.gcp.cloudrunv2.Service;
import com.pulumi.gcp.cloudrunv2.ServiceArgs;
import com.pulumi.gcp.cloudrunv2.ServiceIamMember;
import com.pulumi.gcp.cloudrunv2.ServiceIamMemberArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateContainerArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateContainerEnvsArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateContainerPortsArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateScalingArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateVpcAccessArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateVpcAccessNetworkInterfaceArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTrafficArgs;
import com.pulumi.resources.CustomResourceOptions;

public class CloudRun {

  public static Service create(
      Context ctx,
      Output<String> image,
      Output<String> runtimeSAEmail,
      Output<String> vpcName,
      Output<String> privateSubnetName,
      Output<String> storageBucketName,
      CustomVision customVision) {

    String serviceName = ctx.config().require("cloudRunServiceName");
    String region = ctx.config("gcp").require("region");
    String projectId = ctx.config("gcp").require("project");
    
    // Read Custom Vision config (tightly coupled to Cloud Run)
    String cvProjectId = ctx.config().require("azure-cv-project-id");
    String cvIterationId = ctx.config().require("azure-cv-iteration-id");
    
    // Extract Custom Vision credentials
    Output<String> cvPredictionEndpoint = customVision.getPredictionEndpoint();
    Output<String> cvPredictionKey = customVision.getPredictionKey();

    var cloudRunService = new Service(serviceName,
        ServiceArgs.builder()
            .name(serviceName)
            .location(region)
            .ingress("INGRESS_TRAFFIC_ALL")
            .invokerIamDisabled(true)
            .template(ServiceTemplateArgs.builder()
                .serviceAccount(runtimeSAEmail)
                .vpcAccess(ServiceTemplateVpcAccessArgs.builder()
                    .networkInterfaces(ServiceTemplateVpcAccessNetworkInterfaceArgs.builder()
                        .network(vpcName)
                        .subnetwork(privateSubnetName)
                        .build())
                    .egress("PRIVATE_RANGES_ONLY")
                    .build())
                .scaling(ServiceTemplateScalingArgs.builder()
                    .maxInstanceCount(1)
                    .minInstanceCount(0)
                    .build())
                .containers(ServiceTemplateContainerArgs.builder()
                    .image(image)
                    .ports(ServiceTemplateContainerPortsArgs.builder()
                        .containerPort(8080)
                        .build())
                    .envs(
                        // GCP Storage Bucket
                        ServiceTemplateContainerEnvsArgs.builder()
                            .name("GCS_BUCKET_NAME")
                            .value(storageBucketName)
                            .build(),
                        // Azure Custom Vision - Endpoint
                        ServiceTemplateContainerEnvsArgs.builder()
                            .name("AZURE_CV_PREDICTION_ENDPOINT")
                            .value(cvPredictionEndpoint)
                            .build(),
                        // Azure Custom Vision - Prediction Key (secret)
                        ServiceTemplateContainerEnvsArgs.builder()
                            .name("AZURE_CV_PREDICTION_KEY")
                            .value(cvPredictionKey)
                            .build(),
                        // Azure Custom Vision - Project ID
                        ServiceTemplateContainerEnvsArgs.builder()
                            .name("AZURE_CV_PROJECT_ID")
                            .value(cvProjectId)
                            .build(),
                        // Azure Custom Vision - Iteration ID
                        ServiceTemplateContainerEnvsArgs.builder()
                            .name("AZURE_CV_ITERATION_ID")
                            .value(cvIterationId)
                            .build())
                    .build())
                .build())
            .traffics(ServiceTrafficArgs.builder()
                .type("TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST")
                .percent(100)
                .build())
            .build());

    var publicAccessIam = new ServiceIamMember("allow-public-access", ServiceIamMemberArgs.builder()
        .project(projectId)
        .location(cloudRunService.location())
        .name(cloudRunService.name())
        .role("roles/run.invoker")
        .member("allUsers")
        .build(),
        CustomResourceOptions.builder()
            .dependsOn(cloudRunService)
            .build());

    assert publicAccessIam != null;
    return cloudRunService;
  }
}
