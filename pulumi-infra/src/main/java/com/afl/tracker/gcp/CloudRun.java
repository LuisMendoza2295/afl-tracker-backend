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
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateContainerEnvArgs;
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
    
    // Read Custom Vision Project ID from config (persistent - doesn't change often)
    String cvProjectId = ctx.config().require("azure-cv-project-id");
    System.out.println("[DEBUG] Using azure-cv-project-id: " + cvProjectId);
    
    // Read Custom Vision Iteration ID from config (set by PULUMI_CONFIG_PASSTHROUGH in CI/CD)
    String cvIterationId = ctx.config().require("azure-cv-iteration-id");
    System.out.println("[DEBUG] Using azure-cv-iteration-id: " + cvIterationId);
    
    // Get Custom Vision credentials from Pulumi resource
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
                        ServiceTemplateContainerEnvArgs.builder()
                            .name("GCS_BUCKET_NAME")
                            .value(storageBucketName)
                            .build(),
                        ServiceTemplateContainerEnvArgs.builder()
                            .name("AZURE_CV_PREDICTION_ENDPOINT")
                            .value(cvPredictionEndpoint)
                            .build(),
                        ServiceTemplateContainerEnvArgs.builder()
                            .name("AZURE_CV_PREDICTION_KEY")
                            .value(cvPredictionKey)
                            .build(),
                        ServiceTemplateContainerEnvArgs.builder()
                            .name("AZURE_CV_PROJECT_ID")
                            .value(cvProjectId)
                            .build(),
                        ServiceTemplateContainerEnvArgs.builder()
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
