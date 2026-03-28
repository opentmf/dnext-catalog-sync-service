package org.opentmf.catalog.sync.model;

import static org.opentmf.catalog.sync.model.CatalogConstants.MERGE_PATCH;
import static org.opentmf.catalog.sync.model.EntityType.MULTI_VERSIONED;
import static org.opentmf.catalog.sync.model.EntityType.SINGLE_VERSIONED;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.opentmf.catalog.sync.util.EndpointResolver;
import org.springframework.http.MediaType;

@RequiredArgsConstructor
@Getter
public enum CatalogType {

  PRODUCT_CATEGORY("category", "category", false,
      SINGLE_VERSIONED, MERGE_PATCH, "product/categories"),

  PRODUCT_SPECIFICATION("productSpecification", "productSpecification", true,
      MULTI_VERSIONED, MERGE_PATCH, "product/specifications"),

  PRODUCT_OFFERING_PRICE("productOfferingPrice", "productOfferingPrice", true,
      MULTI_VERSIONED, MERGE_PATCH, "product/prices"),

  PRODUCT_OFFERING("productOffering", "productOffering", true,
      MULTI_VERSIONED, MERGE_PATCH, "product/offerings"),

  PRODUCT_BUNDLES(PRODUCT_OFFERING.getGetEndpoint(), PRODUCT_OFFERING.getPostPatchEndpoint(), true,
      MULTI_VERSIONED, MERGE_PATCH, "product/bundles"),

  /** getEndpoint and postPatchEndpoint are fallbacks when @type is missing or unknown; otherwise resolved from @type. */
  RESOURCE_SPECIFICATION(CatalogConstants.RESOURCE_SPECIFICATION_ENDPOINT,
      CatalogConstants.RESOURCE_SPECIFICATION_ENDPOINT, true,
      MULTI_VERSIONED, MERGE_PATCH, "resource/specifications"),

  SERVICE_SPECIFICATION("serviceSpecification", "serviceSpecification", true,
      MULTI_VERSIONED, MERGE_PATCH, "service/specifications");

  private final String getEndpoint;
  private final String postPatchEndpoint;
  private final boolean patchable;
  private final EntityType entityType;
  private final MediaType patchType;
  private final String locationPattern;

  /**
   * Effective GET endpoint for this type. For RESOURCE_SPECIFICATION, resolved from catalog's
   * {@code @type} (PhysicalResourceSpecification → physicalResourceSpecification, etc.);
   * when {@code @type} is missing or unknown, returns {@link #getEndpoint} (resourceSpecification).
   */
  public String getGetEndpoint(Map<String, Object> catalog) {
    return EndpointResolver.getGetEndpoint(this, catalog);
  }

  public List<String> getGetEndpoints() {
    return EndpointResolver.getGetEndpoints(this);
  }

  /**
   * Effective POST/PATCH endpoint for this type. For RESOURCE_SPECIFICATION, resolved from catalog's
   * {@code @type}; when {@code @type} is missing or unknown, returns {@link #getPostPatchEndpoint()}.
   */
  public String getPostPatchEndpoint(Map<String, Object> catalog) {
    return EndpointResolver.getPostPatchEndpoint(this, catalog);
  }

  public List<String> getPostPatchEndpoints() {
    return EndpointResolver.getPostPatchEndpoints(this);
  }
}
