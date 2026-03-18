package org.opentmf.catalog.sync.util;

import static org.opentmf.catalog.sync.model.CatalogConstants.LOGICAL_RESOURCE_SPECIFICATION_ENDPOINT;
import static org.opentmf.catalog.sync.model.CatalogConstants.PHYSICAL_RESOURCE_SPECIFICATION_ENDPOINT;
import static org.opentmf.catalog.sync.model.CatalogConstants.RESOURCE_SPECIFICATION_ENDPOINT;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentmf.catalog.sync.model.CatalogConstants;
import org.opentmf.catalog.sync.model.CatalogType;

class EndpointResolverTests {

  @Test
  void resourceSpecification_withPhysicalResourceSpecificationType_returnsPhysicalEndpoints() {
    Map<String, Object> catalog = Map.of(CatalogConstants.AT_TYPE,
        CatalogConstants.PHYSICAL_RESOURCE_SPECIFICATION_TYPE);

    Assertions.assertEquals(PHYSICAL_RESOURCE_SPECIFICATION_ENDPOINT,
        EndpointResolver.getGetEndpoint(CatalogType.RESOURCE_SPECIFICATION, catalog));
    Assertions.assertEquals(PHYSICAL_RESOURCE_SPECIFICATION_ENDPOINT,
        EndpointResolver.getPostPatchEndpoint(CatalogType.RESOURCE_SPECIFICATION, catalog));
  }

  @Test
  void resourceSpecification_withLogicalResourceSpecificationType_returnsLogicalEndpoints() {
    Map<String, Object> catalog = Map.of(CatalogConstants.AT_TYPE,
        CatalogConstants.LOGICAL_RESOURCE_SPECIFICATION_TYPE);

    Assertions.assertEquals(LOGICAL_RESOURCE_SPECIFICATION_ENDPOINT,
        EndpointResolver.getGetEndpoint(CatalogType.RESOURCE_SPECIFICATION, catalog));
    Assertions.assertEquals(LOGICAL_RESOURCE_SPECIFICATION_ENDPOINT,
        EndpointResolver.getPostPatchEndpoint(CatalogType.RESOURCE_SPECIFICATION, catalog));
  }

  @Test
  void resourceSpecification_withMissingAtType_fallsBackToEnumEndpoints() {
    Map<String, Object> catalog = Map.of("id", "someId");

    Assertions.assertEquals(RESOURCE_SPECIFICATION_ENDPOINT,
        EndpointResolver.getGetEndpoint(CatalogType.RESOURCE_SPECIFICATION, catalog));
    Assertions.assertEquals(RESOURCE_SPECIFICATION_ENDPOINT,
        EndpointResolver.getPostPatchEndpoint(CatalogType.RESOURCE_SPECIFICATION, catalog));
  }

  @Test
  void resourceSpecification_withNullCatalog_fallsBackToEnumEndpoints() {
    Assertions.assertEquals(RESOURCE_SPECIFICATION_ENDPOINT,
        EndpointResolver.getGetEndpoint(CatalogType.RESOURCE_SPECIFICATION, null));
    Assertions.assertEquals(RESOURCE_SPECIFICATION_ENDPOINT,
        EndpointResolver.getPostPatchEndpoint(CatalogType.RESOURCE_SPECIFICATION, null));
  }

  @Test
  void resourceSpecification_withUnknownAtType_fallsBackToEnumEndpoints() {
    Map<String, Object> catalog = Map.of(CatalogConstants.AT_TYPE, "SomeOtherResourceSpecification");

    Assertions.assertEquals(RESOURCE_SPECIFICATION_ENDPOINT,
        EndpointResolver.getGetEndpoint(CatalogType.RESOURCE_SPECIFICATION, catalog));
    Assertions.assertEquals(RESOURCE_SPECIFICATION_ENDPOINT,
        EndpointResolver.getPostPatchEndpoint(CatalogType.RESOURCE_SPECIFICATION, catalog));
  }

  @Test
  void otherCatalogTypes_returnEnumEndpointsRegardlessOfCatalog() {
    Map<String, Object> catalog = Map.of(CatalogConstants.AT_TYPE,
        CatalogConstants.PHYSICAL_RESOURCE_SPECIFICATION_TYPE);

    for (CatalogType type : CatalogType.values()) {
      if (type == CatalogType.RESOURCE_SPECIFICATION) {
        continue;
      }
      Assertions.assertEquals(type.getGetEndpoint(),
          EndpointResolver.getGetEndpoint(type, catalog),
          "getGetEndpoint for " + type);
      Assertions.assertEquals(type.getPostPatchEndpoint(),
          EndpointResolver.getPostPatchEndpoint(type, catalog),
          "getPostPatchEndpoint for " + type);
    }
  }
}
