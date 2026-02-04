package com.afl.tracker.azure;

/**
 * Azure Built-in Role Definition IDs
 * 
 * These UUIDs are globally consistent across all Azure subscriptions.
 * Reference: https://learn.microsoft.com/en-us/azure/role-based-access-control/built-in-roles
 */
public class AzureRoles {
  
  /**
   * Cognitive Services User - View Cognitive Services resources and perform actions
   */
  public static final String COGNITIVE_SERVICES_USER = "a97b65f3-24c7-4388-baec-2e87135dc908";
  
  /**
   * Cognitive Services Contributor - Full access to Cognitive Services resources
   */
  public static final String COGNITIVE_SERVICES_CONTRIBUTOR = "25fbc0a9-bd7c-42a3-aa1a-3b75d497ee68";
  
  /**
   * Contributor - Full access to manage all resources, but cannot assign roles
   */
  public static final String CONTRIBUTOR = "b24988ac-6180-42a0-ab88-20f7382dd24c";
  
  private AzureRoles() {
    // Prevent instantiation
  }
}
