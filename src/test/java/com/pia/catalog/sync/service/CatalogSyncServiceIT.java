package com.pia.catalog.sync.service;

import static com.pia.catalog.sync.util.CatalogUtil.version0;
import static com.pia.catalog.sync.util.CatalogUtil.version1;
import static com.pia.catalog.sync.util.TypeUtil.asString;
import static com.pia.catalog.sync.util.WebUtil.uri;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.never;
import static org.mockserver.verify.VerificationTimes.once;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pia.catalog.sync.client.impl.CatalogClientImpl;
import com.pia.catalog.sync.config.CatalogSyncProperties;
import com.pia.catalog.sync.exception.CatalogGetException;
import com.pia.catalog.sync.exception.CatalogPatchException;
import com.pia.catalog.sync.exception.CatalogPostException;
import com.pia.catalog.sync.model.CatalogType;
import com.pia.catalog.sync.service.api.CatalogSyncService;
import com.pia.catalog.sync.service.impl.CatalogSyncServiceImpl;
import com.pia.catalog.sync.util.CatalogUtil;
import com.pia.catalog.sync.util.JacksonUtil2;
import com.pia.catalog.sync.util.ResourceUtil;
import com.pia.catalog.sync.util.WebUtil;
import com.pia.client.common.model.BaseClientProperties;
import com.pia.client.common.service.api.TokenService;
import com.pia.commons.util.JacksonUtil;
import com.pia.db.lock.service.api.DbLockService;
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
import org.mockserver.matchers.Times;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Gokhan Demir
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@EnableConfigurationProperties(CatalogSyncProperties.class)
@TestInstance(Lifecycle.PER_CLASS)
@Slf4j
class CatalogSyncServiceIT {

  private static final ClientAndServer MOCK_SERVER = new ClientAndServer();
  private static final String BASE_URL = "http://localhost:" + MOCK_SERVER.getLocalPort();

  @Autowired WebClient webClient;
  @Autowired TokenService tokenService;
  @Autowired BaseClientProperties clientProperties;
  @Autowired DbLockService dbLockService;
  @Autowired CatalogSyncProperties catalogSyncProperties;
  @Autowired private JdbcTemplate jdbcTemplate;

  private CatalogSyncService catalogSyncService;

  @BeforeAll
  void beforeAll() {
    var catalogClient = new CatalogClientImpl(webClient, tokenService, clientProperties);
    catalogSyncService = new CatalogSyncServiceImpl(catalogSyncProperties, dbLockService, catalogClient);
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
    IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
        () -> catalogSyncService.ensureCatalogConsistency());
    Assertions.assertInstanceOf(CatalogGetException.class, e.getCause());
    Assertions.assertEquals("Could not synchronize Catalogs because of exception", e.getMessage());
    Assertions.assertEquals("CatalogGetException: {\"error\": \"Test error\"}",
        ExceptionUtils.getRootCauseMessage(e));
    Throwable cause = ExceptionUtils.getRootCause(e);
    Assertions.assertEquals("CatalogGetException{httpStatus=409 CONFLICT, "
        + "message={\"error\": \"Test error\"}}", cause.toString());
  }

  @Test
  void testSync_withPostException_stopsSynchronizing() {
    get(".*", HttpStatus.NOT_FOUND);
    post(".*", HttpStatus.BAD_REQUEST);
    IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
        () -> catalogSyncService.ensureCatalogConsistency());
    Assertions.assertInstanceOf(CatalogPostException.class, e.getCause());
    Assertions.assertEquals("Could not synchronize Catalogs because of exception", e.getMessage());
    Assertions.assertEquals("CatalogPostException: {\"message\":\"Test Post Error\"}",
        ExceptionUtils.getRootCauseMessage(e));
    Throwable cause = ExceptionUtils.getRootCause(e);
    Assertions.assertEquals("CatalogPostException{httpStatus=400 BAD_REQUEST, "
        + "message={\"message\":\"Test Post Error\"}}", cause.toString());
  }

  @Test
  void testSync_withPatchErrorExpectations_stopSynchronizing() {
    setupAllPatchExpectationsForError();
    IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
        () -> catalogSyncService.ensureCatalogConsistency());
    Assertions.assertInstanceOf(CatalogPatchException.class, e.getCause());
    Assertions.assertEquals("Could not synchronize Catalogs because of exception", e.getMessage());
    Assertions.assertEquals("CatalogPatchException: {\"message\":\"Test Patch Error\"}",
        ExceptionUtils.getRootCauseMessage(e));
    Throwable cause = ExceptionUtils.getRootCause(e);
    Assertions.assertEquals("CatalogPatchException{httpStatus=400 BAD_REQUEST, "
        + "message={\"message\":\"Test Patch Error\"}}", cause.toString());
  }

  private void setupAllGetExpectations() {
    for (CatalogType catalogType : CatalogType.values()) {
      for (Resource resource : CatalogUtil.getCatalogs(catalogType)) {
        Map<String, Object> map = JacksonUtil2.readAsMap(resource);
        String id = asString(map.get("id"));
        setupGetFound(catalogType, id, JacksonUtil.objectToJson(map));
      }
    }
  }

  private void setupAllPatchExpectations() {
    for (CatalogType catalogType : CatalogType.values()) {
      for (Resource resource : CatalogUtil.getCatalogs(catalogType)) {
        String patchResponseBody = ResourceUtil.readAsString(resource);
        String patchRequestBody = CatalogUtil.stripForPatch(JacksonUtil2.readAsMap(patchResponseBody));
        Map<String, Object> map = JacksonUtil2.readAsMap(resource);
        String id = asString(map.get("id"));
        map.put("name", "Different Value");
        setupGetFound(catalogType, id, JacksonUtil.objectToJson(map));
        setupPatchOk(catalogType, id, patchRequestBody, patchResponseBody);
      }
    }
  }

  private void setupAllPatchExpectationsForError() {
    for (CatalogType catalogType : CatalogType.values()) {
      for (Resource resource : CatalogUtil.getCatalogs(catalogType)) {
        String patchResponseBody = ResourceUtil.readAsString(resource);
        String patchRequestBody = CatalogUtil.stripForPatch(JacksonUtil2.readAsMap(patchResponseBody));
        Map<String, Object> map = JacksonUtil2.readAsMap(resource);
        String id = asString(map.get("id"));
        map.put("name", "Different Value");
        setupGetFound(catalogType, id, JacksonUtil.objectToJson(map));
        setupPatchError(catalogType, id, patchRequestBody);
      }
    }
  }

  private void setupAllCreateExpectations() {
    for (CatalogType catalogType : CatalogType.values()) {
      for (Resource resource : CatalogUtil.getCatalogs(catalogType)) {
        String json = ResourceUtil.readAsString(resource);
        Map<String, Object> map = JacksonUtil2.readAsMap(json);
        String requestBody = CatalogUtil.stripForPost(map);
        String id = asString(map.get("id"));
        setupGetNotFound(catalogType, id);
        switch (catalogType.getEntityType()) {
          case MULTI_VERSIONED -> {
            setupPostOk(catalogType, stripValidFor(version0(requestBody)), version0(json));
            setupPostOk(catalogType, stripValidFor(version1(requestBody)), version1(json));
          }
          case SINGLE_VERSIONED -> {
            setupPostOk(catalogType, stripValidFor(version1(requestBody)), version1(json));
          }
          case NORMAL -> {
            setupPostOk(catalogType, requestBody, json);
          }
        }
      }
    }
  }

  private String stripValidFor(String requestBody) {
    var tree = (ObjectNode) com.pia.commons.util.JacksonUtil.jsonToTree(requestBody);
    tree.remove("validFor");
    return com.pia.commons.util.JacksonUtil.objectToJson(tree);
  }

  private void setupGetNotFound(CatalogType catalogType, String id) {
    URI uri = WebUtil.uri(BASE_URL, catalogType.getGetEndpoint(), id);
    get(uri.getPath(), HttpStatus.NOT_FOUND);
  }

  private void setupGetFound(CatalogType catalogType, String id, String body) {
    URI uri = WebUtil.uri(BASE_URL, catalogType.getGetEndpoint(), id);
    get(uri.getPath(), HttpStatus.OK, body);
  }

  private void setupPostOk(CatalogType catalogType, String requestBody, String responseBody) {
    URI uri = uri(BASE_URL, catalogType.getPostPatchEndpoint());
    post(uri.getPath(), requestBody, HttpStatus.CREATED, responseBody);
  }

  private void setupPatchOk(CatalogType catalogType, String id, String requestBody,
      String responseBody) {
    URI uri = uri(BASE_URL, catalogType.getPostPatchEndpoint(), id);
    patch(uri.getPath(), MediaType.parse(catalogType.getPatchType().toString()),
        requestBody, HttpStatus.OK, responseBody);
  }

  private void setupPatchError(CatalogType catalogType, String id, String requestBody) {
    URI uri = uri(BASE_URL, catalogType.getPostPatchEndpoint(), id);
    patch(uri.getPath(), MediaType.parse(catalogType.getPatchType().toString()),
        requestBody, HttpStatus.BAD_REQUEST, "{\"message\":\"Test Patch Error\"}");
  }

  private void verifyAllPatchExpectations() {
    for (CatalogType catalogType : CatalogType.values()) {
      for (Resource resource : CatalogUtil.getCatalogs(catalogType)) {
        Map<String, Object> map = JacksonUtil2.readAsMap(resource);
        String id = asString(map.get("id"));
        URI uri = uri(BASE_URL, catalogType.getPostPatchEndpoint(), id);
        if (catalogType.isPatchable()) {
          MOCK_SERVER.verify(
              request().withMethod("PATCH").withPath(uri.getPath()), once()
          );
        } else {
          MOCK_SERVER.verify(
              request().withMethod("PATCH").withPath(uri.getPath()), VerificationTimes.never()
          );
        }
      }
    }
  }

  private void verifyAllGetExpectations() {
    for (CatalogType catalogType : CatalogType.values()) {
      for (Resource resource : CatalogUtil.getCatalogs(catalogType)) {
        Map<String, Object> map = JacksonUtil2.readAsMap(resource);
        String id = asString(map.get("id"));
        URI uri = uri(BASE_URL, catalogType.getGetEndpoint(), id);
        MOCK_SERVER.verify(request().withMethod("GET").withPath(uri.getPath()), once());
        MOCK_SERVER.verify(request().withMethod("PATCH").withPath(uri.getPath()), never());
      }
    }
  }

  private static void post(String path, String requestBody, HttpStatus responseStatus,
      String responseBody) {
    MOCK_SERVER
        .when(
            request()
                .withMethod("POST")
                .withPath(path)
                .withBody(new JsonBody(requestBody, MatchType.ONLY_MATCHING_FIELDS)),
            Times.once())
        .respond(
            response()
                .withBody(responseBody)
                .withStatusCode(responseStatus.value()));
  }

  private static void post(String path, HttpStatus responseStatus) {
    MOCK_SERVER
        .when(
            request()
                .withMethod("POST")
                .withPath(path),
            Times.once())
        .respond(
            response()
                .withBody("{\"message\":\"Test Post Error\"}")
                .withStatusCode(responseStatus.value()));
  }

  private static void patch(String path, MediaType contentType, String requestBody,
      HttpStatus responseStatus,
      String responseBody) {
    MOCK_SERVER
        .when(
            request()
                .withMethod("PATCH")
                .withPath(path)
                .withContentType(contentType)
                .withBody(requestBody),
            Times.once())
        .respond(
            response()
                .withBody(responseBody)
                .withStatusCode(responseStatus.value()));
  }

  private static void get(String path, HttpStatus responseStatus) {
    MOCK_SERVER
        .when(
            request()
                .withMethod("GET")
                .withPath(path),
            Times.once())
        .respond(
            response()
                .withStatusCode(responseStatus.value()));
  }

  private static void get(String path, HttpStatus responseStatus, String responseBody) {
    MOCK_SERVER
        .when(
            request()
                .withMethod("GET")
                .withPath(path),
            Times.once())
        .respond(
            response()
                .withStatusCode(responseStatus.value())
                .withBody(responseBody));
  }
}
