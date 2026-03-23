package org.opentmf.catalog.sync.client.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opentmf.catalog.sync.exception.CatalogGetException;
import org.opentmf.catalog.sync.exception.CatalogPatchException;
import org.opentmf.catalog.sync.exception.CatalogPostException;
import org.opentmf.client.common.exception.OpenTmfClientResponseException;
import org.opentmf.client.common.model.ClientProperties;
import org.opentmf.client.rest.service.api.SyncTokenService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class RestCatalogClientImplTest {

  @Mock private RestTemplate restTemplate;
  @Mock private SyncTokenService tokenService;

  private RestCatalogClientImpl client;

  @BeforeEach
  void setUp() {
    var props = new ClientProperties();
    props.setNumRetries(0);
    props.setRetryWaitMillis(100);
    client = new RestCatalogClientImpl(restTemplate, tokenService, props);
  }

  @Test
  void get_returnsBody() {
    when(tokenService.getToken()).thenReturn("tok");
    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"id\":\"1\"}"));
    String result = client.get(URI.create("http://localhost/api")).block();
    assertEquals("{\"id\":\"1\"}", result);
  }

  @Test
  void get_onError_throwsCatalogGetException() {
    when(tokenService.getToken()).thenReturn("tok");
    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenThrow(new OpenTmfClientResponseException(HttpStatus.NOT_FOUND, "not found"));
    var ex = assertThrows(CatalogGetException.class, () -> client.get(URI.create("http://l/a")).block());
    assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatusCode());
  }

  @Test
  void post_returnsBody() {
    when(tokenService.getToken()).thenReturn("tok");
    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body("{\"id\":\"1\"}"));
    String result = client.post(URI.create("http://localhost/api"), "{}").block();
    assertEquals("{\"id\":\"1\"}", result);
  }

  @Test
  void post_onError_throwsCatalogPostException() {
    when(tokenService.getToken()).thenReturn("tok");
    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
        .thenThrow(new OpenTmfClientResponseException(HttpStatus.BAD_REQUEST, "bad"));
    var ex = assertThrows(CatalogPostException.class, () -> client.post(URI.create("http://l/a"), "{}").block());
    assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
  }

  @Test
  void patch_returnsBody() {
    when(tokenService.getToken()).thenReturn("tok");
    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.PATCH), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"id\":\"1\"}"));
    String result = client.patch(URI.create("http://localhost/api"), MediaType.APPLICATION_JSON, "{}").block();
    assertEquals("{\"id\":\"1\"}", result);
  }

  @Test
  void patch_onError_throwsCatalogPatchException() {
    when(tokenService.getToken()).thenReturn("tok");
    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.PATCH), any(HttpEntity.class), eq(String.class)))
        .thenThrow(new OpenTmfClientResponseException(HttpStatus.CONFLICT, "conflict"));
    var ex = assertThrows(CatalogPatchException.class,
        () -> client.patch(URI.create("http://l/a"), MediaType.APPLICATION_JSON, "{}").block());
    assertEquals(HttpStatus.CONFLICT, ex.getHttpStatusCode());
  }
}
