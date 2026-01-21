package com.afl.tracker.vision;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Import;
import com.pulumi.resources.ResourceArgs;

public class VisionResourceArgs extends ResourceArgs {

  @Import(name="project")
  public String project;

  @Import(name="location")
  public String location;

  // Fields for ProductSet
  @Import(name="productSetId")
  public String productSetId;

  @Import(name="displayName")
  public String displayName;

  // Fields for Product
  @Import(name="productId")
  public String productId;

  @Import(name="productCategory")
  public String productCategory;

  // Fields for ReferenceImage
  @Import(name="referenceImageId")
  public String referenceImageId;

  @Import(name="uri")
  public Output<String> uri;
}
