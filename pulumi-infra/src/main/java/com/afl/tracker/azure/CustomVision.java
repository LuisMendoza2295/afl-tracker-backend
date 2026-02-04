package com.afl.tracker.azure;

import com.pulumi.Context;
import com.pulumi.azurenative.cognitiveservices.Account;
import com.pulumi.azurenative.cognitiveservices.AccountArgs;
import com.pulumi.azurenative.cognitiveservices.CognitiveservicesFunctions;
import com.pulumi.azurenative.cognitiveservices.enums.PublicNetworkAccess;
import com.pulumi.azurenative.cognitiveservices.inputs.AccountPropertiesArgs;
import com.pulumi.azurenative.cognitiveservices.inputs.ListAccountKeysArgs;
import com.pulumi.azurenative.cognitiveservices.inputs.SkuArgs;
import com.pulumi.azurenative.authorization.RoleAssignment;
import com.pulumi.azurenative.authorization.RoleAssignmentArgs;
import com.pulumi.azurenative.authorization.enums.PrincipalType;
import com.pulumi.core.Output;
import com.pulumi.resources.StackReference;

import java.util.Map;

public class CustomVision {
  
  private final Output<String> trainingEndpoint;
  private final Output<String> predictionEndpoint;
  private final Output<String> trainingKey;
  private final Output<String> predictionKey;

  public CustomVision(Context ctx, StackReference platformStack) {
    String env = ctx.stackName();
    var location = ctx.config("azure-native").require("location");
    var subscriptionId = ctx.config("azure-native").require("subscriptionId");
    
    // Get shared resources from platform stack
    var resourceGroupName = platformStack.requireOutput("azureResourceGroupName").applyValue(v -> v.toString());
    var backendIdentityPrincipalId = platformStack.requireOutput("azureBackendIdentityPrincipalId").applyValue(v -> v.toString());
    
    // Create Custom Vision Training Account
    var cvTraining = new Account("custom-vision-training",
        AccountArgs.builder()
            .accountName(String.format("afl-cv-train-%s", env))
            .resourceGroupName(resourceGroupName)
            .location(location)
            .kind("CustomVision.Training")
            .sku(SkuArgs.builder()
                .name("F0")  // Free tier, change to S0 for production
                .build())
            .properties(AccountPropertiesArgs.builder()
                .customSubDomainName(String.format("afl-cv-train-%s", env))
                .publicNetworkAccess(PublicNetworkAccess.Enabled)
                .build())
            .tags(Map.of(
                "environment", env,
                "service", "custom-vision-training"
            ))
            .build());
    
    // Create Custom Vision Prediction Account
    var cvPrediction = new Account("custom-vision-prediction",
        AccountArgs.builder()
            .accountName(String.format("afl-cv-pred-%s", env))
            .resourceGroupName(resourceGroupName)
            .location(location)
            .kind("CustomVision.Prediction")
            .sku(SkuArgs.builder()
                .name("F0")  // Free tier
                .build())
            .properties(AccountPropertiesArgs.builder()
                .customSubDomainName(String.format("afl-cv-pred-%s", env))
                .publicNetworkAccess(PublicNetworkAccess.Enabled)
                .build())
            .tags(Map.of(
                "environment", env,
                "service", "custom-vision-prediction"
            ))
            .build());
    
    // Cognitive Services User role ID (well-known UUID)
    var cognitiveServicesUserRoleId = Output.format(
        "/subscriptions/%s/providers/Microsoft.Authorization/roleDefinitions/%s",
        subscriptionId, AzureRoles.COGNITIVE_SERVICES_USER);
    
    // Grant backend identity access to Custom Vision Training
    var trainingAccess = new RoleAssignment("backend-cv-training-access",
        RoleAssignmentArgs.builder()
            .principalId(backendIdentityPrincipalId)
            .principalType(PrincipalType.ServicePrincipal)
            .roleDefinitionId(cognitiveServicesUserRoleId)
            .scope(cvTraining.id())
            .build());
    
    // Grant backend identity access to Custom Vision Prediction
    var predictionAccess = new RoleAssignment("backend-cv-prediction-access",
        RoleAssignmentArgs.builder()
            .principalId(backendIdentityPrincipalId)
            .principalType(PrincipalType.ServicePrincipal)
            .roleDefinitionId(cognitiveServicesUserRoleId)
            .scope(cvPrediction.id())
            .build());
    
    // Export endpoints and keys
    this.trainingEndpoint = cvTraining.properties().applyValue(p -> p.endpoint());
    this.predictionEndpoint = cvPrediction.properties().applyValue(p -> p.endpoint());
    
    this.trainingKey = cvTraining.name().apply(name -> 
        resourceGroupName.apply(rgName -> 
            CognitiveservicesFunctions.listAccountKeys(ListAccountKeysArgs.builder()
                .accountName(name)
                .resourceGroupName(rgName)
                .build())
        ).applyValue(keysOutput -> keysOutput.key1().orElse(""))
    );
    
    this.predictionKey = cvPrediction.name().apply(name -> 
        resourceGroupName.apply(rgName -> 
            CognitiveservicesFunctions.listAccountKeys(ListAccountKeysArgs.builder()
                .accountName(name)
                .resourceGroupName(rgName)
                .build())
        ).applyValue(keysOutput -> keysOutput.key1().orElse(""))
    );
    
    // Ensure role assignments are created (even though not used directly)
    assert trainingAccess != null;
    assert predictionAccess != null;
  }

  public Output<String> getTrainingEndpoint() {
    return trainingEndpoint;
  }

  public Output<String> getPredictionEndpoint() {
    return predictionEndpoint;
  }

  public Output<String> getTrainingKey() {
    return trainingKey.asSecret();
  }

  public Output<String> getPredictionKey() {
    return predictionKey.asSecret();
  }
}
