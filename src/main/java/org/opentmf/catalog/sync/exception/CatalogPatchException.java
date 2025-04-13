package org.opentmf.catalog.sync.exception;

import org.springframework.http.HttpStatus;

/**
 * @author Gokhan Demir
 */
public class CatalogPatchException extends CatalogSyncException {

  public CatalogPatchException(HttpStatus httpStatus, String message) {
    super(httpStatus, message);
  }
}
