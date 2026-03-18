package org.opentmf.catalog.sync.util;

import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.opentmf.catalog.sync.model.CatalogConstants;
import org.opentmf.catalog.sync.model.CatalogType;

import static org.opentmf.catalog.sync.model.CatalogConstants.*;

/**
 * Resolves effective get and post-patch endpoints for {@link CatalogType#RESOURCE_SPECIFICATION}
 * from the catalog payload's {@link CatalogConstants#AT_TYPE}.
 * When {@code @type} is missing or not one of the expected values, falls back to the enum's
 * static values ({@link CatalogConstants#RESOURCE_SPECIFICATION_ENDPOINT} for both).
 */
public final class EndpointResolver {

  @Generated
  private EndpointResolver() {
  }

  /**
   * Returns the effective GET endpoint for the given catalog type and payload.
   * For RESOURCE_SPECIFICATION, resolves from {@code @type}; otherwise returns the type's getEndpoint.
   */
  public static String getGetEndpoint(CatalogType catalogType, Map<String, Object> catalog) {
    if (catalogType != CatalogType.RESOURCE_SPECIFICATION) {
      return catalogType.getGetEndpoint();
    }
    return resolveResourceSpecificationEndpoint(catalog, catalogType.getGetEndpoint());
  }

  public static List<String> getGetEndpoints(CatalogType type) {
    if (type == CatalogType.RESOURCE_SPECIFICATION) {
      return List.of(PHYSICAL_RESOURCE_SPECIFICATION_ENDPOINT, LOGICAL_RESOURCE_SPECIFICATION_ENDPOINT);
    }
    return List.of(type.getGetEndpoint());
  }

  /**
   * Returns the effective POST/PATCH endpoint for the given catalog type and payload.
   * For RESOURCE_SPECIFICATION, resolves from {@code @type}; otherwise returns the type's postPatchEndpoint.
   */
  public static String getPostPatchEndpoint(CatalogType catalogType, Map<String, Object> catalog) {
    if (catalogType != CatalogType.RESOURCE_SPECIFICATION) {
      return catalogType.getPostPatchEndpoint();
    }
    return resolveResourceSpecificationEndpoint(catalog, catalogType.getPostPatchEndpoint());
  }

  public static List<String> getPostPatchEndpoints(CatalogType type) {
    if (type == CatalogType.RESOURCE_SPECIFICATION) {
      return List.of(PHYSICAL_RESOURCE_SPECIFICATION_ENDPOINT, LOGICAL_RESOURCE_SPECIFICATION_ENDPOINT);
    }
    return List.of(type.getPostPatchEndpoint());
  }

  private static String resolveResourceSpecificationEndpoint(Map<String, Object> catalog,
      String fallback) {
    if (catalog == null) {
      return fallback;
    }
    String atType = TypeUtil.asString(catalog.get(CatalogConstants.AT_TYPE));
    if (PHYSICAL_RESOURCE_SPECIFICATION_TYPE.equals(atType)) {
      return PHYSICAL_RESOURCE_SPECIFICATION_ENDPOINT;
    }
    if (LOGICAL_RESOURCE_SPECIFICATION_TYPE.equals(atType)) {
      return LOGICAL_RESOURCE_SPECIFICATION_ENDPOINT;
    }
    return fallback;
  }
}
