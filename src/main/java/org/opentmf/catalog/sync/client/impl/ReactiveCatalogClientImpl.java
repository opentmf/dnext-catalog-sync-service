package org.opentmf.catalog.sync.client.impl;

import java.net.URI;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.opentmf.catalog.sync.client.api.CatalogClient;
import org.opentmf.catalog.sync.exception.CatalogGetException;
import org.opentmf.catalog.sync.exception.CatalogPatchException;
import org.opentmf.catalog.sync.exception.CatalogPostException;
import org.opentmf.catalog.sync.exception.CatalogSyncException;
import org.opentmf.client.common.model.ClientProperties;
import org.opentmf.client.reactive.service.api.TokenService;
import org.opentmf.client.reactive.util.WebClientUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Reactive {@link CatalogClient} implementation backed by {@link WebClient}.
 *
 * @author Gokhan Demir
 */
@RequiredArgsConstructor
public class ReactiveCatalogClientImpl implements CatalogClient {

  private final WebClient webClient;
  private final TokenService tokenService;
  private final ClientProperties clientProperties;

  @Override
  public Mono<String> get(URI uri) {
    return tokenService.getToken()
        .flatMap(token -> webClient.get().uri(uri)
            .headers(h -> h.setBearerAuth(token))
            .retrieve()
            .onStatus(HttpStatusCode::isError, r -> handleError(r, CatalogGetException.class))
            .bodyToMono(String.class)
            .retryWhen(retry()));
  }

  @Override
  public Mono<String> post(URI uri, String body) {
    return tokenService.getToken()
        .flatMap(token -> webClient.post().uri(uri)
            .headers(h -> h.setBearerAuth(token))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .onStatus(HttpStatusCode::isError, r -> handleError(r, CatalogPostException.class))
            .bodyToMono(String.class)
            .retryWhen(retry()));
  }

  @Override
  public Mono<String> patch(URI uri, MediaType patchType, String body) {
    return tokenService.getToken()
        .flatMap(token -> webClient.patch().uri(uri)
            .headers(h -> h.setBearerAuth(token))
            .contentType(patchType)
            .bodyValue(body)
            .retrieve()
            .onStatus(HttpStatusCode::isError, r -> handleError(r, CatalogPatchException.class))
            .bodyToMono(String.class)
            .retryWhen(retry()));
  }

  private reactor.util.retry.RetryBackoffSpec retry() {
    return WebClientUtil.retry(
        clientProperties.getNumRetries(),
        Duration.ofMillis(clientProperties.getRetryWaitMillis()));
  }

  private static Mono<CatalogSyncException> handleError(
      ClientResponse clientResponse, Class<? extends CatalogSyncException> clazz) {
    HttpStatusCode httpStatusCode = clientResponse.statusCode();
    return clientResponse.bodyToMono(String.class)
        .map(rawBody -> createException(httpStatusCode, rawBody, clazz))
        .switchIfEmpty(Mono.defer(() -> {
          HttpStatus resolved = HttpStatus.resolve(httpStatusCode.value());
          String reason = resolved != null ? resolved.getReasonPhrase()
              : "Unknown Status " + httpStatusCode.value();
          return Mono.error(createException(httpStatusCode, reason, clazz));
        }));
  }

  private static CatalogSyncException createException(HttpStatusCode httpStatusCode, String message,
      Class<? extends CatalogSyncException> clazz) {
    try {
      return clazz.getDeclaredConstructor(HttpStatusCode.class, String.class)
          .newInstance(httpStatusCode, message);
    } catch (Exception e) {
      throw new CatalogSyncException(httpStatusCode, message);
    }
  }
}
