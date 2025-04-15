package org.opentmf.catalog.sync.exception;

import org.springframework.http.HttpStatusCode;

/**
 * @author Gokhan Demir
 */
public class CatalogPostException extends CatalogSyncException {

  public CatalogPostException(HttpStatusCode httpStatusCode, String message) {
    super(httpStatusCode, message);
  }
}
