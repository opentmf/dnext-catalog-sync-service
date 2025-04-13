package org.opentmf.catalog.sync.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Gokhan Demir
 */
@RequiredArgsConstructor
@Getter
public final class Catalog {

  private final CatalogType type;
  private final String id;
  private final String name;
  private final String version;
  private final Long revision;

  @Override
  public String toString() {
    return String.format("%s id: %s name: %s, version: %s, revision: %d",
        type, id, name, version, revision);
  }
}
