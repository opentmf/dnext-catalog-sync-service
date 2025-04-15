package org.opentmf.catalog.sync.exception;

import org.springframework.http.HttpStatusCode;

/**
 * @author Gokhan Demir
 */
public class CatalogGetException extends CatalogSyncException {

  public CatalogGetException(HttpStatusCode httpStatusCode, String message) {
    super(httpStatusCode, message);
  }
}
