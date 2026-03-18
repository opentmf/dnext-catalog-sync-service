package org.opentmf.catalog.sync.client.impl;

import static org.opentmf.catalog.sync.util.CatalogUtil.version0;
import static org.opentmf.catalog.sync.util.CatalogUtil.version1;
import static org.opentmf.catalog.sync.util.WebUtil.uri;

import org.opentmf.catalog.sync.client.api.CatalogClient;
import org.opentmf.catalog.sync.exception.CatalogGetException;
import org.opentmf.catalog.sync.exception.CatalogPatchException;
import org.opentmf.catalog.sync.exception.CatalogPostException;
import org.opentmf.catalog.sync.model.CatalogType;
import org.opentmf.catalog.sync.model.SingleContext;
import org.opentmf.catalog.sync.util.CatalogUtil;
import org.opentmf.catalog.sync.util.WebUtil;
import org.opentmf.client.common.model.BaseClientProperties;
import org.opentmf.client.common.service.api.TokenService;
import org.opentmf.client.common.util.WebClientUtil;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.opentmf.commons.util.JacksonUtil;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author Gokhan Demir
 */
@RequiredArgsConstructor
public class CatalogClientImpl implements CatalogClient {

  private final WebClient webClient;
  private final TokenService tokenService;
  private final BaseClientProperties clientProperties;

  @Override
  public Mono<String> get(String baseUrl, SingleContext context) {
    URI uri = uri(baseUrl, getGetEndpoint(context.getCatalogType(), context.getId()),
        context.getId());
    return tokenService.getToken()
        .flatMap(token -> get(uri, token));
  }

  @Override
  public Mono<String> post(String baseUrl, SingleContext context) {
    URI uri = uri(baseUrl, getPostPatchEndpoint(context.getCatalogType(), context.getId()));
    String body = CatalogUtil.stripForPost(context.getRequestedCatalog());
    return tokenService
        .getToken()
        .flatMap(
            token ->
                switch (context.getCatalogType().getEntityType()) {
                  case MULTI_VERSIONED -> postMultiVersioned(uri, body, token);
                  case SINGLE_VERSIONED -> postSingleVersioned(uri, body, token);
                  case NORMAL -> post(uri, body, token);
                });
  }

  private Mono<String> postMultiVersioned(URI uri, String json, String token) {
    return post(uri, version0(json), token)
        .map(response -> CatalogUtil.version1(json))
        .flatMap(version1 -> post(uri, version1, token));
  }

  private Mono<String> postSingleVersioned(URI uri, String json, String token) {
    return post(uri, version1(json), token);
  }

  @Override
  public Mono<String> patch(String baseUrl, SingleContext context) {
    URI uri = uri(baseUrl,
        getPostPatchEndpoint(context.getCatalogType(), context.getId()),
        context.getId());
    String body = CatalogUtil.stripForPatch(context.getRequestedCatalog());
    return tokenService.getToken()
        .flatMap(token -> mergePatch(uri, context.getCatalogType().getPatchType(), body, token));
  }

  private Mono<String> get(URI uri, String accessToken) {
    return webClient.get().uri(uri)
        .headers(headers -> headers.setBearerAuth(accessToken))
        .retrieve()
        .onStatus(HttpStatusCode::isError, r -> WebUtil.handleError(r, CatalogGetException.class))
        .bodyToMono(String.class)
        .retryWhen(WebClientUtil.retry(clientProperties.getNumRetries(),
            Duration.of(clientProperties.getRetryWaitMillis(), ChronoUnit.MILLIS)));
  }

  private Mono<String> post(URI uri, Object body, String accessToken) {
    return webClient
        .post()
        .uri(uri)
        .headers(headers -> headers.setBearerAuth(accessToken))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .onStatus(HttpStatusCode::isError, r -> WebUtil.handleError(r, CatalogPostException.class))
        .bodyToMono(String.class)
        .retryWhen(WebClientUtil.retry(clientProperties.getNumRetries(),
            Duration.ofMillis(clientProperties.getRetryWaitMillis())));
  }

  private Mono<String> mergePatch(URI uri, MediaType mediaType, Object body, String accessToken) {
    return webClient
        .patch()
        .uri(uri)
        .headers(headers -> headers.setBearerAuth(accessToken))
        .contentType(mediaType)
        .bodyValue(body)
        .retrieve()
        .onStatus(HttpStatusCode::isError, r -> WebUtil.handleError(r, CatalogPatchException.class))
        .bodyToMono(String.class)
        .retryWhen(WebClientUtil.retry(clientProperties.getNumRetries(),
            Duration.ofMillis(clientProperties.getRetryWaitMillis())));
  }

  private String getPostPatchEndpoint(CatalogType catalogType, String id) {
    if (catalogType != CatalogType.RESOURCE_SPECIFICATION) {
      return catalogType.getPostPatchEndpoint();
    }
    return catalogType.getPostPatchEndpoint(getJson(catalogType, id));
  }

  private String getGetEndpoint(CatalogType catalogType, String id) {
    if (catalogType != CatalogType.RESOURCE_SPECIFICATION) {
      return catalogType.getGetEndpoint();
    }
    return catalogType.getGetEndpoint(getJson(catalogType, id));
  }

  private Map<String, Object> getJson(CatalogType catalogType, String id) {
    var path =  "catalog/" + catalogType.getLocationPattern() + "/" + id + ".json";
    return JacksonUtil.jsonToMap(JacksonUtil.contents(path));
  }

}
