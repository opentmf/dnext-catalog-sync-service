package com.pia.catalog.sync.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author Gokhan Demir
 */
@Getter
@Setter
@RequiredArgsConstructor
public final class OverallContext {

  private final List<Catalog> createdCatalogs = new ArrayList<>();
  private final List<Catalog> updatedCatalogs = new ArrayList<>();

  public void addCreatedCatalog(Catalog catalog) {
    createdCatalogs.add(catalog);
  }

  public void addUpdatedCatalog(Catalog catalog) {
    updatedCatalogs.add(catalog);
  }

  public int getTouchedCount() {
    return createdCatalogs.size() + updatedCatalogs.size();
  }
}
