package org.opentmf.catalog.sync.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;

/**
 * @author Gokhan Demir
 */
@RequiredArgsConstructor
@Getter
public class CatalogSyncException extends RuntimeException {

  private final HttpStatusCode httpStatusCode;
  private final String message;

  @Override
  public String toString() {
    return this.getClass().getSimpleName() +
        "{httpStatus=" + httpStatusCode +
        ", message=" + message + "}";
  }
}
