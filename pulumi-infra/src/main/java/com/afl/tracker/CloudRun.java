package com.afl.tracker;

import java.util.Map;

import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.gcp.cloudrunv2.Service;
import com.pulumi.gcp.cloudrunv2.ServiceArgs;
import com.pulumi.gcp.cloudrunv2.ServiceIamMember;
import com.pulumi.gcp.cloudrunv2.ServiceIamMemberArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateContainerArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateContainerPortsArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateScalingArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateVpcAccessArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateVpcAccessNetworkInterfaceArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTrafficArgs;

public class CloudRun {

  public static Service createCloudRunService(Context ctx, Output<String> image, Output<String> runtimeSAEmail, Output<String> vpcName, Output<String> privateSubnetName) {
    String serviceName = ctx.config().require("cloudRunServiceName");
    String region = ctx.config("gcp").require("region");
    String projectId = ctx.config("gcp").require("project");
    var cloudRunService = new Service(serviceName,
        ServiceArgs.builder()
            .name(serviceName)
            .location(region)
            .ingress("INGRESS_TRAFFIC_ALL")
            .template(ServiceTemplateArgs.builder()
                .serviceAccount(runtimeSAEmail)
                .vpcAccess(ServiceTemplateVpcAccessArgs.builder()
                    .networkInterfaces(ServiceTemplateVpcAccessNetworkInterfaceArgs.builder()
                        .network(vpcName)
                        .subnetwork(privateSubnetName)
                        .build())
                    .egress("PRIVATE_RANGES_ONLY")
                    .build())
                .annotations(Map.of(
                    "run.googleapis.com/invoker-iam-disabled", "true"
                ))
                .scaling(ServiceTemplateScalingArgs.builder()
                    .maxInstanceCount(2)
                    .minInstanceCount(1)
                    .build())
                .containers(ServiceTemplateContainerArgs.builder()
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

    new ServiceIamMember("allow-public-access", ServiceIamMemberArgs.builder()
        .project(projectId)
        .location(cloudRunService.location())
        .name(cloudRunService.name())
        .role("roles/run.invoker")
        .member("allUsers")
        .build());
    return cloudRunService;
  }
}
