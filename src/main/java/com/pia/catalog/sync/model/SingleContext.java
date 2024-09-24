package com.pia.catalog.sync.model;

import java.util.SortedMap;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SingleContext {

  private CatalogType catalogType;
  private String id;
  private SortedMap<String, Object> requestedCatalog;
  private SortedMap<String, Object> existingCatalog;
}
