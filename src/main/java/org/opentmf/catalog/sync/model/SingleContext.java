package org.opentmf.catalog.sync.model;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SingleContext {

  private CatalogType catalogType;
  private String id;
  private Map<String, Object> requestedCatalog;
  private Map<String, Object> existingCatalog;
  private String existingVersion;
}
