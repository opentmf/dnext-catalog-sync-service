package org.opentmf.catalog.sync.util;

import org.opentmf.catalog.sync.exception.CatalogSyncException;
import java.net.URI;
import java.util.Arrays;
import lombok.Generated;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * @author Gokhan Demir
 */
public final class WebUtil {

  @Generated
  private WebUtil() {
  }

  public static URI uri(String path, String... segment) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(path);
    Arrays.stream(segment).forEach(builder::pathSegment);
    return builder.build().toUri();
  }

  public static Mono<CatalogSyncException> handleError(
      ClientResponse clientResponse, Class<? extends CatalogSyncException> clazz) {
    HttpStatusCode httpStatusCode = clientResponse.statusCode();
    return clientResponse.bodyToMono(String.class)
        .map(rawBody -> createException(httpStatusCode, rawBody, clazz))
        .switchIfEmpty(Mono.defer(() ->
            Mono.error(createException(httpStatusCode, HttpStatus.resolve(httpStatusCode.value()).getReasonPhrase(), clazz))));
  }

  private static CatalogSyncException createException(HttpStatusCode httpStatusCode, String message,
      Class<? extends CatalogSyncException> clazz) {
    try {
      return clazz
          .getDeclaredConstructor(HttpStatusCode.class, String.class)
          .newInstance(httpStatusCode, message);
    } catch (Exception e) {
      throw new CatalogSyncException(httpStatusCode, message);
    }
  }
}
