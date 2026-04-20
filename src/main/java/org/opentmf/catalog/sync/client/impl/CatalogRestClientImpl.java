package org.opentmf.catalog.sync.client.impl;

import java.net.URI;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.opentmf.catalog.sync.client.api.CatalogRestClient;
import org.opentmf.catalog.sync.exception.CatalogGetException;
import org.opentmf.catalog.sync.exception.CatalogPatchException;
import org.opentmf.catalog.sync.exception.CatalogPostException;
import org.opentmf.catalog.sync.exception.CatalogSyncException;
import org.opentmf.client.common.exception.OpenTmfClientResponseException;
import org.opentmf.client.common.model.ClientProperties;
import org.opentmf.client.rest.service.api.SyncTokenService;
import org.opentmf.client.rest.util.SyncClientUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Synchronous {@link CatalogRestClient} implementation backed by {@link RestClient}.
 *
 * @author Gokhan Demir
 */
@RequiredArgsConstructor
public class CatalogRestClientImpl implements CatalogRestClient {

  private final RestClient restClient;
  private final SyncTokenService tokenService;
  private final ClientProperties clientProperties;

  @Override
  public String get(URI uri) {
    return execute(() -> restClient.get().uri(uri)
        .headers(this::applyAuth)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve().body(String.class), CatalogGetException.class);
  }

  @Override
  public String post(URI uri, String body) {
    return execute(() -> restClient.post().uri(uri)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .headers(this::applyAuth)
        .body(body)
        .retrieve().body(String.class), CatalogPostException.class);
  }

  @Override
  public String patch(URI uri, MediaType patchType, String body) {
    return execute(() -> restClient.patch().uri(uri)
        .contentType(patchType)
        .accept(MediaType.APPLICATION_JSON)
        .headers(this::applyAuth)
        .body(body)
        .retrieve().body(String.class), CatalogPatchException.class);
  }

  private String execute(Supplier<String> action,
      Class<? extends CatalogSyncException> errorClass) {
    try {
      return SyncClientUtil.executeWithRetry(action,
          clientProperties.getNumRetries(),
          clientProperties.getRetryWaitDuration());
    } catch (OpenTmfClientResponseException e) {
      throw createException(e, errorClass);
    }
  }

  private void applyAuth(HttpHeaders headers) {
    String tokenType = tokenService.getTokenType();
    if (tokenType.isEmpty()) {
      return;
    }
    String token = tokenService.getToken();
    if (!token.isEmpty()) {
      headers.set(HttpHeaders.AUTHORIZATION, tokenType + " " + token);
    }
  }

  private static CatalogSyncException createException(OpenTmfClientResponseException source,
      Class<? extends CatalogSyncException> clazz) {
    try {
      return clazz.getDeclaredConstructor(HttpStatusCode.class, String.class)
          .newInstance(source.getStatusCode(), source.getMessage());
    } catch (Exception e) {
      throw new CatalogSyncException(source.getStatusCode(), source.getMessage());
    }
  }
}
