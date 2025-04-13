package org.opentmf.catalog.sync.model;

import static org.opentmf.catalog.sync.model.EntityType.MULTI_VERSIONED;
import static org.opentmf.catalog.sync.model.EntityType.SINGLE_VERSIONED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;

@RequiredArgsConstructor
@Getter
public enum CatalogType {

  PRODUCT_CATEGORY("category", "category", false,
      SINGLE_VERSIONED, CatalogConstants.MERGE_PATCH, "product/categories"),

  PRODUCT_SPECIFICATION("productSpecification", "productSpecification", true,
      MULTI_VERSIONED, CatalogConstants.MERGE_PATCH, "product/specifications"),

  PRODUCT_OFFERING("productOffering", "productOffering", true,
      MULTI_VERSIONED, CatalogConstants.MERGE_PATCH, "product/offerings"),

  PRODUCT_BUNDLES(PRODUCT_OFFERING.getGetEndpoint(), PRODUCT_OFFERING.getPostPatchEndpoint(), true,
      MULTI_VERSIONED, CatalogConstants.MERGE_PATCH, "product/bundles"),

  RESOURCE_SPECIFICATION("resourceSpecification", "physicalResourceSpecification", true,
      MULTI_VERSIONED, APPLICATION_JSON, "resource/specifications"),

  SERVICE_SPECIFICATION("serviceSpecification", "serviceSpecification", true,
      MULTI_VERSIONED, CatalogConstants.MERGE_PATCH, "service/specifications");

  private final String getEndpoint;
  private final String postPatchEndpoint;
  private final boolean patchable;
  private final EntityType entityType;
  private final MediaType patchType;
  private final String locationPattern;
}
