package org.opentmf.catalog.sync.model;

import lombok.Generated;
import org.springframework.http.MediaType;

/**
 * @author Gokhan Demir
 */
public final class CatalogConstants {

  @Generated
  private CatalogConstants() {
  }

  public static final MediaType MERGE_PATCH = MediaType.parseMediaType("application/merge-patch+json");

  public static final String VALID_FOR = "validFor";
  public static final String VERSION = "version";
  public static final String HREF = "href";
  public static final String REVISION = "revision";
  public static final String ACL_RELATED_PARTY = "aclRelatedParty";
  public static final String LAST_UPDATE = "lastUpdate";
  public static final String CREATED_DATE = "createdDate";
  public static final String UPDATED_DATE = "updatedDate";
  public static final String CREATED_BY = "createdBy";
  public static final String UPDATED_BY = "updatedBy";
  public static final String AT_SCHEMA_LOCATION = "@schemaLocation";
  public static final String ID = "id";
  public static final String AT_BASE_TYPE = "@baseType";
  public static final String AT_TYPE = "@type";

  // Resource specification endpoint resolution (@type → get/postPatch path segment)
  public static final String PHYSICAL_RESOURCE_SPECIFICATION_TYPE = "PhysicalResourceSpecification";
  public static final String LOGICAL_RESOURCE_SPECIFICATION_TYPE = "LogicalResourceSpecification";
  public static final String PHYSICAL_RESOURCE_SPECIFICATION_ENDPOINT = "physicalResourceSpecification";
  public static final String LOGICAL_RESOURCE_SPECIFICATION_ENDPOINT = "logicalResourceSpecification";
  /** Fallback when @type is missing or unknown; also the enum's static get/postPatch value for RESOURCE_SPECIFICATION. */
  public static final String RESOURCE_SPECIFICATION_ENDPOINT = "resourceSpecification";
}
