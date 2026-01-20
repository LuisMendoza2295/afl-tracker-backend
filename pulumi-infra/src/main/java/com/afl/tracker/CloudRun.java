package com.afl.tracker;

import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.gcp.cloudrunv2.Service;
import com.pulumi.gcp.cloudrunv2.ServiceArgs;
import com.pulumi.gcp.cloudrunv2.ServiceIamBinding;
import com.pulumi.gcp.cloudrunv2.ServiceIamBindingArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateContainerArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateContainerPortsArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTemplateScalingArgs;
import com.pulumi.gcp.cloudrunv2.inputs.ServiceTrafficArgs;
import com.pulumi.gcp.serviceaccount.Account;

public class CloudRun {

  public static Service createCloudRunService(Context ctx, Output<String> image, Account runtimeSA) {
    String serviceName = ctx.config().require("cloudRunServiceName");
    String region = ctx.config("gcp").require("region");
    var cloudRunService = new Service(serviceName,
        ServiceArgs.builder()
            .name(serviceName)
            .location(region)
            .ingress("INGRESS_TRAFFIC_ALL")
            .template(ServiceTemplateArgs.builder()
                .serviceAccount(runtimeSA.email())
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

    new ServiceIamBinding("public-access", ServiceIamBindingArgs.builder()
        .location(cloudRunService.location())
        .name(cloudRunService.name())
        .role("roles/run.invoker")
        .members("allUsers")
        .build());
    return cloudRunService;
  }
}
