package com.pia.catalog.sync.exception;

import org.springframework.http.HttpStatus;

/**
 * @author Gokhan Demir
 */
public class CatalogPostException extends CatalogSyncException {

  public CatalogPostException(HttpStatus httpStatus, String message) {
    super(httpStatus, message);
  }
}
