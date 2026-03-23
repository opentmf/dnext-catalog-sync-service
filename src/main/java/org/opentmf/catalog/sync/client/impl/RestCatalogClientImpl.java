package org.opentmf.catalog.sync.client.impl;

import java.net.URI;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.opentmf.catalog.sync.client.api.CatalogClient;
import org.opentmf.catalog.sync.exception.CatalogGetException;
import org.opentmf.catalog.sync.exception.CatalogPatchException;
import org.opentmf.catalog.sync.exception.CatalogPostException;
import org.opentmf.catalog.sync.exception.CatalogSyncException;
import org.opentmf.client.common.exception.OpenTmfClientResponseException;
import org.opentmf.client.common.model.ClientProperties;
import org.opentmf.client.rest.service.api.SyncTokenService;
import org.opentmf.client.rest.util.RestTemplateUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

/**
 * Synchronous {@link CatalogClient} implementation backed by {@link RestTemplate}.
 * Each call is wrapped in {@link Mono#fromCallable} so the service layer can use a
 * single reactive pipeline regardless of transport.
 *
 * @author Gokhan Demir
 */
@RequiredArgsConstructor
public class RestCatalogClientImpl implements CatalogClient {

  private final RestTemplate restTemplate;
  private final SyncTokenService tokenService;
  private final ClientProperties clientProperties;

  @Override
  public Mono<String> get(URI uri) {
    return Mono.fromCallable(() -> execute(uri, HttpMethod.GET, null, null, CatalogGetException.class));
  }

  @Override
  public Mono<String> post(URI uri, String body) {
    return Mono.fromCallable(() -> execute(uri, HttpMethod.POST, MediaType.APPLICATION_JSON, body,
        CatalogPostException.class));
  }

  @Override
  public Mono<String> patch(URI uri, MediaType patchType, String body) {
    return Mono.fromCallable(() -> execute(uri, HttpMethod.PATCH, patchType, body,
        CatalogPatchException.class));
  }

  private String execute(URI uri, HttpMethod method, MediaType contentType, String body,
      Class<? extends CatalogSyncException> errorClass) {
    try {
      String token = tokenService.getToken();
      return RestTemplateUtil.executeWithRetry(
          () -> doExchange(uri, method, contentType, body, token),
          clientProperties.getNumRetries(),
          Duration.ofMillis(clientProperties.getRetryWaitMillis()));
    } catch (OpenTmfClientResponseException e) {
      throw createException(e, errorClass);
    }
  }

  private String doExchange(URI uri, HttpMethod method, MediaType contentType, String body,
      String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    if (contentType != null) {
      headers.setContentType(contentType);
    }
    HttpEntity<String> entity = body != null ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);
    ResponseEntity<String> response = restTemplate.exchange(uri, method, entity, String.class);
    return response.getBody();
  }

  private static CatalogSyncException createException(OpenTmfClientResponseException source,
      Class<? extends CatalogSyncException> clazz) {
    try {
      return clazz.getDeclaredConstructor(
              org.springframework.http.HttpStatusCode.class, String.class)
          .newInstance(source.getStatusCode(), source.getMessage());
    } catch (Exception e) {
      throw new CatalogSyncException(source.getStatusCode(), source.getMessage());
    }
  }
}
