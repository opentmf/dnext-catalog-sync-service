package com.pia.catalog.sync.exception;

import org.springframework.http.HttpStatus;

/**
 * @author Gokhan Demir
 */
public class CatalogGetException extends CatalogSyncException {

  public CatalogGetException(HttpStatus httpStatus, String message) {
    super(httpStatus, message);
  }
}
