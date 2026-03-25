package org.opentmf.catalog.sync.client.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opentmf.catalog.sync.exception.CatalogGetException;
import org.opentmf.catalog.sync.exception.CatalogPatchException;
import org.opentmf.catalog.sync.exception.CatalogPostException;
import org.opentmf.client.common.model.ClientProperties;
import org.opentmf.client.rest.service.api.SyncTokenService;
import org.opentmf.client.rest.util.OpenTmfRestClientStatusHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class CatalogRestClientImplTest {

  @Mock private SyncTokenService tokenService;

  private MockRestServiceServer mockServer;
  private CatalogRestClientImpl client;

  @BeforeEach
  void setUp() {
    var builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    var restClient = builder
        .defaultStatusHandler(HttpStatusCode::isError,
            OpenTmfRestClientStatusHandler.errorHandler())
        .build();
    var props = new ClientProperties();
    props.setNumRetries(0);
    props.setRetryWaitDuration(java.time.Duration.ofMillis(100));
    client = new CatalogRestClientImpl(restClient, tokenService, props);
  }

  @Test
  void get_returnsBody() {
    stubToken();
    mockServer.expect(requestTo("http://localhost/api"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok"))
        .andRespond(withSuccess("{\"id\":\"1\"}", MediaType.APPLICATION_JSON));

    String result = client.get(URI.create("http://localhost/api"));
    assertEquals("{\"id\":\"1\"}", result);
    mockServer.verify();
  }

  @Test
  void get_onError_throwsCatalogGetException() {
    stubToken();
    mockServer.expect(requestTo("http://localhost/api"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND).body("not found"));

    var ex = assertThrows(CatalogGetException.class,
        () -> client.get(URI.create("http://localhost/api")));
    assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatusCode());
    mockServer.verify();
  }

  @Test
  void post_returnsBody() {
    stubToken();
    mockServer.expect(requestTo("http://localhost/api"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok"))
        .andRespond(withStatus(HttpStatus.CREATED)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"id\":\"1\"}"));

    String result = client.post(URI.create("http://localhost/api"), "{}");
    assertEquals("{\"id\":\"1\"}", result);
    mockServer.verify();
  }

  @Test
  void post_onError_throwsCatalogPostException() {
    stubToken();
    mockServer.expect(requestTo("http://localhost/api"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("bad"));

    var ex = assertThrows(CatalogPostException.class,
        () -> client.post(URI.create("http://localhost/api"), "{}"));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
    mockServer.verify();
  }

  @Test
  void patch_returnsBody() {
    stubToken();
    mockServer.expect(requestTo("http://localhost/api"))
        .andExpect(method(HttpMethod.PATCH))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok"))
        .andRespond(withSuccess("{\"id\":\"1\"}", MediaType.APPLICATION_JSON));

    String result = client.patch(URI.create("http://localhost/api"),
        MediaType.APPLICATION_JSON, "{}");
    assertEquals("{\"id\":\"1\"}", result);
    mockServer.verify();
  }

  @Test
  void patch_onError_throwsCatalogPatchException() {
    stubToken();
    mockServer.expect(requestTo("http://localhost/api"))
        .andExpect(method(HttpMethod.PATCH))
        .andRespond(withStatus(HttpStatus.CONFLICT).body("conflict"));

    var ex = assertThrows(CatalogPatchException.class,
        () -> client.patch(URI.create("http://localhost/api"),
            MediaType.APPLICATION_JSON, "{}"));
    assertEquals(HttpStatus.CONFLICT, ex.getHttpStatusCode());
    mockServer.verify();
  }

  private void stubToken() {
    when(tokenService.getTokenType()).thenReturn("Bearer");
    when(tokenService.getToken()).thenReturn("tok");
  }
}
