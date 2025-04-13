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


}
