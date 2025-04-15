package org.opentmf.catalog.sync.exception;

import org.springframework.http.HttpStatusCode;

/**
 * @author Gokhan Demir
 */
public class CatalogPatchException extends CatalogSyncException {

  public CatalogPatchException(HttpStatusCode httpStatusCode, String message) {
    super(httpStatusCode, message);
  }
}
