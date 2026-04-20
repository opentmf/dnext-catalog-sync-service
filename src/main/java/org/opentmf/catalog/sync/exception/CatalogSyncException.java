package org.opentmf.catalog.sync.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * @author Gokhan Demir
 */
@Getter
public class CatalogSyncException extends RuntimeException {

  private final HttpStatusCode httpStatusCode;

  public CatalogSyncException(HttpStatusCode httpStatusCode, String message) {
    super(message);
    this.httpStatusCode = httpStatusCode;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{httpStatus=" + httpStatusCode
        + ", message=" + getMessage() + "}";
  }
}
