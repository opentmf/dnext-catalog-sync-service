package org.opentmf.catalog.sync.service;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.never;
import static org.mockserver.verify.VerificationTimes.once;
import static org.opentmf.catalog.sync.util.CatalogUtil.launchedVersion;
import static org.opentmf.catalog.sync.util.CatalogUtil.version0;
import static org.opentmf.catalog.sync.util.TypeUtil.asString;
import static org.opentmf.catalog.sync.util.WebUtil.uri;

import java.net.URI;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.opentmf.catalog.sync.client.impl.CatalogReactiveClientImpl;
import org.opentmf.catalog.sync.config.CatalogSyncProperties;
import org.opentmf.catalog.sync.exception.CatalogGetException;
import org.opentmf.catalog.sync.exception.CatalogPatchException;
import org.opentmf.catalog.sync.exception.CatalogPostException;
import org.opentmf.catalog.sync.model.CatalogConstants;
import org.opentmf.catalog.sync.model.CatalogType;
import org.opentmf.catalog.sync.model.EntityType;
import org.opentmf.catalog.sync.service.api.CatalogSyncService;
import org.opentmf.catalog.sync.service.impl.ReactiveCatalogSyncServiceImpl;
import org.opentmf.catalog.sync.util.CatalogUtil;
import org.opentmf.catalog.sync.util.ResourceUtil;
import org.opentmf.catalog.sync.util.WebUtil;
import org.opentmf.client.common.model.ClientProperties;
import org.opentmf.commons.util.JacksonUtil;
import org.opentmf.db.lock.service.api.DbLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Gokhan Demir
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@EnableConfigurationProperties(CatalogSyncProperties.class)
@TestInstance(Lifecycle.PER_CLASS)
@Slf4j
class CatalogSyncReactiveIT {

  private static final ClientAndServer MOCK_SERVER = new ClientAndServer();
  private static final String BASE_URL = "http://localhost:" + MOCK_SERVER.getLocalPort();

  @Autowired private DbLockService dbLockService;
  @Autowired private CatalogSyncProperties catalogSyncProperties;
  @Autowired private JdbcTemplate jdbcTemplate;

  private CatalogSyncService catalogSyncService;

  @BeforeAll
  void beforeAll() {
    var webClient = WebClient.builder().build();
    var tokenService = new org.opentmf.client.reactive.service.impl.NoOpTokenService();
    var clientProperties = new ClientProperties();
    clientProperties.setNumRetries(3);
    clientProperties.setRetryWaitDuration(java.time.Duration.ofMillis(100));
    var catalogClient = new CatalogReactiveClientImpl(webClient, tokenService, clientProperties);
    catalogSyncService =
        new ReactiveCatalogSyncServiceImpl(catalogSyncProperties, dbLockService, catalogClient);
    catalogSyncProperties.setProductCatalogUrl(BASE_URL);
    catalogSyncProperties.setResourceCatalogUrl(BASE_URL);
    catalogSyncProperties.setServiceCatalogUrl(BASE_URL);
  }

  @BeforeEach
  void beforeEach() {
    jdbcTemplate.execute("truncate table DB_LOCK_LATEST");
    log.info("Initializing the Catalog Sync Service.");
  }

  @AfterEach
  void afterEach() {
    log.info("Completed initializing the Catalog Sync Service.");
    if (MOCK_SERVER.isRunning()) {
      MOCK_SERVER.reset();
    }
  }

  @Test
  void testSync_withAlreadySynchronizedVersion_skipsCatalogSynchronization() {
    jdbcTemplate.execute(
        "insert into DB_LOCK_LATEST (lock_type, lock_version, hostname, lock_acquired_on) "
            + "values ('C', '1.0.0', 'localhost', current_timestamp)");
    Assertions.assertDoesNotThrow(() -> catalogSyncService.ensureCatalogConsistency());
  }

  @Test
  void testSync_withNonExistentCatalogs_createsAllCatalogs() {
    setupAllCreateExpectations();
    Assertions.assertDoesNotThrow(() -> catalogSyncService.ensureCatalogConsistency());
  }

  @Test
  void testSync_withDifferentContentExistingCatalogs_patchesPatchableCatalogs() {
    setupAllPatchExpectations();
    Assertions.assertDoesNotThrow(() -> catalogSyncService.ensureCatalogConsistency());
    verifyAllPatchExpectations();
  }

  @Test
  void testSync_withSameContentExistingCatalogs_doesNotPatchAnyCatalog() {
    setupAllGetExpectations();
    Assertions.assertDoesNotThrow(() -> catalogSyncService.ensureCatalogConsistency());
    verifyAllGetExpectations();
  }

  @Test
  void TestSync_withGetException_stopsSynchronizing() {
    get(".*", HttpStatus.CONFLICT, "{\"error\": \"Test error\"}");
    IllegalStateException e =
        Assertions.assertThrows(
            IllegalStateException.class, () -> catalogSyncService.ensureCatalogConsistency());
    Assertions.assertInstanceOf(CatalogGetException.class, e.getCause());
    Assertions.assertEquals("Could not synchronize Catalogs because of exception", e.getMessage());
    Assertions.assertEquals(
        "CatalogGetException: {\"error\": \"Test error\"}", ExceptionUtils.getRootCauseMessage(e));
    Throwable cause = ExceptionUtils.getRootCause(e);
    Assertions.assertEquals(
        "CatalogGetException{httpStatus=409 CONFLICT, " + "message={\"error\": \"Test error\"}}",
        cause.toString());
  }

  @Test
  void testSync_withPostException_stopsSynchronizing() {
    get(".*", HttpStatus.NOT_FOUND);
    post(".*", HttpStatus.BAD_REQUEST);
    IllegalStateException e =
        Assertions.assertThrows(
            IllegalStateException.class, () -> catalogSyncService.ensureCatalogConsistency());
    Assertions.assertInstanceOf(CatalogPostException.class, e.getCause());
    Assertions.assertEquals("Could not synchronize Catalogs because of exception", e.getMessage());
    Assertions.assertEquals(
        "CatalogPostException: {\"message\":\"Test Post Error\"}",
        ExceptionUtils.getRootCauseMessage(e));
    Throwable cause = ExceptionUtils.getRootCause(e);
    Assertions.assertEquals(
        "CatalogPostException{httpStatus=400 BAD_REQUEST, "
            + "message={\"message\":\"Test Post Error\"}}",
        cause.toString());
  }

  @Test
  void testSync_withPatchErrorExpectations_stopSynchronizing() {
    setupAllPatchExpectationsForError();
    IllegalStateException e =
        Assertions.assertThrows(
            IllegalStateException.class, () -> catalogSyncService.ensureCatalogConsistency());
    Assertions.assertInstanceOf(CatalogPatchException.class, e.getCause());
    Assertions.assertEquals("Could not synchronize Catalogs because of exception", e.getMessage());
    Assertions.assertEquals(
        "CatalogPatchException: {\"message\":\"Test Patch Error\"}",
        ExceptionUtils.getRootCauseMessage(e));
    Throwable cause = ExceptionUtils.getRootCause(e);
    Assertions.assertEquals(
        "CatalogPatchException{httpStatus=400 BAD_REQUEST, "
            + "message={\"message\":\"Test Patch Error\"}}",
        cause.toString());
  }

  private void setupAllGetExpectations() {
    for (CatalogType catalogType : CatalogType.values()) {
      for (Resource resource : CatalogUtil.getCatalogs(catalogType)) {
        Map<String, Object> map = CatalogUtil.readAsMap(resource);
        String id = asString(map.get("id"));
        ensureLaunchedVersion(catalogType, map);
        setupGetFound(catalogType, id, JacksonUtil.objectToJson(map));
      }
    }
  }

  private void setupAllPatchExpectations() {
    for (CatalogType catalogType : CatalogType.values()) {
      for (Resource resource : CatalogUtil.getCatalogs(catalogType)) {
        String patchResponseBody = ResourceUtil.readAsString(resource);
        String patchRequestBody =
            CatalogUtil.stripForPatch(CatalogUtil.readAsMap(patchResponseBody));
        Map<String, Object> map = CatalogUtil.readAsMap(resource);
        String id = asString(map.get("id"));
        map.put("name", "Different Value");
        String existingVersion = ensureLaunchedVersion(catalogType, map);
        setupGetFound(catalogType, id, JacksonUtil.objectToJson(map));
        String patchId = multiVersionedId(catalogType, id, existingVersion);
        setupPatchOk(catalogType, patchId, patchRequestBody, patchResponseBody);
      }
    }
  }

  private void setupAllPatchExpectationsForError() {
    for (CatalogType catalogType : CatalogType.values()) {
      for (Resource resource : CatalogUtil.getCatalogs(catalogType)) {
        String patchResponseBody = ResourceUtil.readAsString(resource);
        String patchRequestBody =
            CatalogUtil.stripForPatch(CatalogUtil.readAsMap(patchResponseBody));
        Map<String, Object> map = CatalogUtil.readAsMap(resource);
        String id = asString(map.get("id"));
        map.put("name", "Different Value");
        String existingVersion = ensureLaunchedVersion(catalogType, map);
        setupGetFound(catalogType, id, JacksonUtil.objectToJson(map));
        String patchId = multiVersionedId(catalogType, id, existingVersion);
        setupPatchError(catalogType, patchId, patchRequestBody);
      }
    }
  }

  /**
   * For MULTI_VERSIONED entities, the GET response must carry a non-zero version so that
   * the sync service treats it as a launched version. Returns the version string.
   */
  private String ensureLaunchedVersion(CatalogType catalogType, Map<String, Object> map) {
    if (catalogType.getEntityType() != EntityType.MULTI_VERSIONED) {
      return null;
    }
    String version = asString(map.get(CatalogConstants.VERSION));
    if (version == null || "0".equals(version)) {
      map.put(CatalogConstants.VERSION, "1");
      return "1";
    }
    return version;
  }

  private String multiVersionedId(CatalogType catalogType, String id, String version) {
    if (catalogType.getEntityType() == EntityType.MULTI_VERSIONED && version != null) {
      return id + ":(version=" + version + ")";
    }
    return id;
  }

  private void setupAllCreateExpectations() {
    for (CatalogType catalogType : CatalogType.values()) {
      for (Resource resource : CatalogUtil.getCatalogs(catalogType)) {
        String json = ResourceUtil.readAsString(resource);
        Map<String, Object> map = CatalogUtil.readAsMap(json);
        String requestBody = CatalogUtil.stripForPost(map);
        String id = asString(map.get("id"));
        setupGetNotFound(catalogType, id);
        switch (catalogType.getEntityType()) {
          case MULTI_VERSIONED -> {
            setupPostOk(catalogType, stripValidFor(version0(requestBody)), version0(json));
            setupPostOk(catalogType, stripValidFor(launchedVersion(requestBody)), launchedVersion(json));
          }
          case SINGLE_VERSIONED ->
            setupPostOk(catalogType, stripValidFor(launchedVersion(requestBody)), launchedVersion(json));
          case NORMAL ->
            setupPostOk(catalogType, requestBody, json);
        }
      }
    }
  }

  private String stripValidFor(String requestBody) {
    var tree = (ObjectNode) JacksonUtil.jsonToTree(requestBody);
    tree.remove("validFor");
    return JacksonUtil.objectToJson(tree);
  }

  private void setupGetNotFound(CatalogType catalogType, String id) {
    for (String endpoint : catalogType.getGetEndpoints()) {
      URI uri = WebUtil.uri(BASE_URL, endpoint, id);
      get(uri.getPath(), HttpStatus.NOT_FOUND);
    }
  }

  private void setupGetFound(
      CatalogType catalogType, String id, String body) {
    for (String endpoint : catalogType.getGetEndpoints()) {
      URI uri = WebUtil.uri(BASE_URL, endpoint, id);
      get(uri.getPath(), HttpStatus.OK, body);
    }
  }

  private void setupPostOk(
      CatalogType catalogType,
      String requestBody,
      String responseBody) {
    for (String endpoint : catalogType.getPostPatchEndpoints()) {
      URI uri = uri(BASE_URL, endpoint);
      post(uri.getPath(), requestBody, HttpStatus.CREATED, responseBody);
    }
  }

  private void setupPatchOk(
      CatalogType catalogType,
      String id,
      String requestBody,
      String responseBody) {
    for (String endpoint : catalogType.getPostPatchEndpoints()) {
      URI uri = uri(BASE_URL, endpoint, id);
      patch(
          uri.getPath(),
          MediaType.parse(catalogType.getPatchType().toString()),
          requestBody,
          HttpStatus.OK,
          responseBody);
    }
  }

  private void setupPatchError(
      CatalogType catalogType, String id, String requestBody) {
    for (String endpoint : catalogType.getPostPatchEndpoints()) {
      URI uri = uri(BASE_URL, endpoint, id);
      patch(uri.getPath(), MediaType.parse(catalogType.getPatchType().toString()),
          requestBody,
          HttpStatus.BAD_REQUEST,
          "{\"message\":\"Test Patch Error\"}");
    }
  }

  private void verifyAllPatchExpectations() {
    for (CatalogType catalogType : CatalogType.values()) {
      for (Resource resource : CatalogUtil.getCatalogs(catalogType)) {
        Map<String, Object> map = CatalogUtil.readAsMap(resource);
        String id = asString(map.get("id"));
        String version = ensureLaunchedVersion(catalogType, map);
        String patchId = multiVersionedId(catalogType, id, version);
        URI uri = uri(BASE_URL, catalogType.getPostPatchEndpoint(map), patchId);
        if (catalogType.isPatchable()) {
          MOCK_SERVER.verify(request().withMethod("PATCH").withPath(uri.getPath()), once());
        } else {
          MOCK_SERVER.verify(request().withMethod("PATCH").withPath(uri.getPath()), never());
        }
      }
    }
  }

  private void verifyAllGetExpectations() {
    for (CatalogType catalogType : CatalogType.values()) {
      for (Resource resource : CatalogUtil.getCatalogs(catalogType)) {
        Map<String, Object> map = CatalogUtil.readAsMap(resource);
        String id = asString(map.get("id"));
        URI uri = uri(BASE_URL, catalogType.getGetEndpoint(map), id);
        MOCK_SERVER.verify(request().withMethod("GET").withPath(uri.getPath()), once());
      }
    }
  }

  private static void post(
      String path, String requestBody, HttpStatus responseStatus, String responseBody) {
    MOCK_SERVER
        .when(
            request()
                .withMethod("POST")
                .withPath(path)
                .withBody(new JsonBody(requestBody, MatchType.ONLY_MATCHING_FIELDS)))
        .respond(response().withBody(responseBody).withStatusCode(responseStatus.value()));
  }

  private static void post(String path, HttpStatus responseStatus) {
    MOCK_SERVER
        .when(request().withMethod("POST").withPath(path))
        .respond(
            response()
                .withBody("{\"message\":\"Test Post Error\"}")
                .withStatusCode(responseStatus.value()));
  }

  private static void patch(
      String path,
      MediaType contentType,
      String requestBody,
      HttpStatus responseStatus,
      String responseBody) {
    MOCK_SERVER
        .when(
            request()
                .withMethod("PATCH")
                .withPath(path)
                .withContentType(contentType)
                .withBody(requestBody))
        .respond(response().withBody(responseBody).withStatusCode(responseStatus.value()));
  }

  private static void get(String path, HttpStatus responseStatus) {
    MOCK_SERVER
        .when(request().withMethod("GET").withPath(path))
        .respond(response().withStatusCode(responseStatus.value()));
  }

  private static void get(String path, HttpStatus responseStatus, String responseBody) {
    MOCK_SERVER
        .when(request().withMethod("GET").withPath(path))
        .respond(response().withStatusCode(responseStatus.value())
            .withBody(responseBody, MediaType.APPLICATION_JSON));
  }
}
