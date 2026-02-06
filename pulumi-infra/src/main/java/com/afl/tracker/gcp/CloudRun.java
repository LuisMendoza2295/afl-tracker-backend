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
    
    // Read Custom Vision Iteration ID from PULUMI_CONFIG or config (set dynamically by CI/CD)
    String cvIterationId = getPulumiConfigValue("afl-tracker-backend:azure-cv-iteration-id");
    if (cvIterationId != null && !cvIterationId.isEmpty()) {
      System.out.println("[DEBUG] Using azure-cv-iteration-id from PULUMI_CONFIG: " + cvIterationId);
    } else {
      cvIterationId = ctx.config().get("azure-cv-iteration-id").orElse(null);
      if (cvIterationId == null || cvIterationId.isEmpty()) {
        throw new IllegalStateException("azure-cv-iteration-id must be set via PULUMI_CONFIG or Pulumi config");
      }
      System.out.println("[DEBUG] Using azure-cv-iteration-id from Pulumi config: " + cvIterationId);
    }
    
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
  
  /**
   * Parse PULUMI_CONFIG environment variable to get config value.
   * PULUMI_CONFIG is a JSON object like: {"key": "value", ...}
   */
  private static String getPulumiConfigValue(String key) {
    String pulumiConfig = System.getenv("PULUMI_CONFIG");
    if (pulumiConfig == null || pulumiConfig.isEmpty()) {
      return null;
    }
    
    // Simple JSON parsing without external dependencies
    String searchKey = "\"" + key + "\":";
    int keyIndex = pulumiConfig.indexOf(searchKey);
    if (keyIndex == -1) {
      return null;
    }
    
    int valueStart = pulumiConfig.indexOf("\"", keyIndex + searchKey.length());
    if (valueStart == -1) {
      return null;
    }
    
    int valueEnd = pulumiConfig.indexOf("\"", valueStart + 1);
    if (valueEnd == -1) {
      return null;
    }
    
    return pulumiConfig.substring(valueStart + 1, valueEnd);
  }
}
