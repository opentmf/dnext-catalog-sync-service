package org.opentmf.catalog.sync.service.impl;

import static org.opentmf.catalog.sync.util.WebUtil.uri;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opentmf.catalog.sync.client.api.CatalogClient;
import org.opentmf.catalog.sync.config.CatalogSyncProperties;
import org.opentmf.catalog.sync.exception.CatalogGetException;
import org.opentmf.catalog.sync.exception.CatalogPatchException;
import org.opentmf.catalog.sync.exception.CatalogPostException;
import org.opentmf.catalog.sync.model.Catalog;
import org.opentmf.catalog.sync.model.CatalogConstants;
import org.opentmf.catalog.sync.model.CatalogType;
import org.opentmf.catalog.sync.model.EntityType;
import org.opentmf.catalog.sync.model.OverallContext;
import org.opentmf.catalog.sync.model.SingleContext;
import org.opentmf.catalog.sync.service.api.CatalogSyncService;
import org.opentmf.catalog.sync.util.CatalogUtil;
import org.opentmf.catalog.sync.util.EndpointResolver;
import org.opentmf.catalog.sync.util.TypeUtil;
import org.opentmf.commons.util.JacksonUtil;
import org.opentmf.commons.util.UrlUtil;
import org.opentmf.db.lock.exception.DbLockException;
import org.opentmf.db.lock.model.AcquiredLock;
import org.opentmf.db.lock.model.LockType;
import org.opentmf.db.lock.service.api.DbLockService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Gokhan Demir
 */
@Slf4j
@RequiredArgsConstructor
public class CatalogSyncServiceImpl implements CatalogSyncService {

  private static final String PRODUCT = "product";
  public static final String RESOURCE = "resource";
  public static final String SERVICE = "service";

  private final CatalogSyncProperties catalogSyncProperties;
  private final DbLockService dbLockService;
  private final CatalogClient catalogClient;

  @Override
  public void ensureCatalogConsistency() {
    try {
      ensureRequiredUrlsProvided();
      doEnsureCatalogConsistency();
    } catch (DbLockException e) {
      throw new IllegalStateException("", e);
    }
  }

  private void ensureRequiredUrlsProvided() {
    ensureValidUrl(CatalogUtil.getCatalogs(PRODUCT), catalogSyncProperties.getProductCatalogUrl(), PRODUCT);
    ensureValidUrl(CatalogUtil.getCatalogs(RESOURCE), catalogSyncProperties.getResourceCatalogUrl(), RESOURCE);
    ensureValidUrl(CatalogUtil.getCatalogs(SERVICE), catalogSyncProperties.getServiceCatalogUrl(), SERVICE);
  }

  private void ensureValidUrl(Resource[] catalogs, String baseUrl, String module) {
    if (catalogs.length == 0) {
      if (StringUtils.hasText(baseUrl)) {
        log.warn("{} catalog URL provided, although there are no resources.", module);
      }
      return;
    }
    Assert.hasText(baseUrl, module + " catalog URL must be provided.");
    UrlUtil.ensureHttpUrl(baseUrl);
  }

  private void doEnsureCatalogConsistency() throws DbLockException {
    OverallContext context = new OverallContext();
    boolean lockReleased = false;
    AcquiredLock lock = null;
    try {
      String requestedVersion = catalogSyncProperties.getCatalogVersion();
      lock = dbLockService.acquireLock(LockType.CATALOG, requestedVersion);
      if (lock.isUpgrade()
          || (lock.isDowngrade()
              && lock.isDowngradeAllowed(catalogSyncProperties.getDowngradeAllowedAfter())))
      {
        doSync(context);
        releaseLock(lock, context.getTouchedCount());
        lockReleased = true;
      } else {
        dbLockService.releaseLock(lock, false);
        lockReleased = true;
        log.info("Catalog files are already up-to-date for version {}.", lock.getLockVersion());
      }
    } catch (Exception e) {
      dbLockService.releaseLock(lock, false);
      lockReleased = true;
      throw new IllegalStateException("Could not synchronize Catalogs because of exception", e);
    } finally {
      if (!lockReleased) {
        releaseLock(lock, context.getTouchedCount());
      }
    }
  }

  private void doSync(OverallContext context) {
    syncResourceSpecifications(context)
        .then(Mono.defer(() -> syncServiceSpecifications(context)))
        .then(Mono.defer(() -> syncProductCategories(context)))
        .then(Mono.defer(() -> syncProductSpecifications(context)))
        .then(Mono.defer(() -> syncProductOfferings(context)))
        .then(Mono.defer(() -> syncBundles(context)))
        .block();
    logDeploymentDetails(context);
  }

  private void logDeploymentDetails(OverallContext context) {
    int touchedCount = context.getTouchedCount();
    int resourceCount = CatalogUtil.getAllCatalogs().length;
    if (touchedCount == 0) {
      log.warn("Catalog synchronization completed without updating any Catalog. "
              + "The specified catalogVersion was: {}. "
              + "Hint: Do not change the catalogVersion when there are no Catalog changes.",
          catalogSyncProperties.getCatalogVersion());
    } else {
      log.info("Catalog synchronization for version {} has been completed. "
              + "Updated Catalog count is {} out of the total {}",
          catalogSyncProperties.getCatalogVersion(), touchedCount, resourceCount);
    }
    if (touchedCount > 0 && log.isDebugEnabled()) {
      log.debug("Deployed Catalogs and Their Versions follow:");
      for (Catalog catalog : context.getCreatedCatalogs()) {
        log.debug(String.format("Created: %s", catalog));
      }
      for (Catalog catalog : context.getUpdatedCatalogs()) {
        log.debug(String.format("Updated: %s", catalog));
      }
    }
  }

  // ── Sync entry points per catalog domain ──────────────────────────────

  private Mono<Void> syncProductCategories(OverallContext context) {
    return sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_CATEGORY, CatalogUtil.getCatalogs(CatalogType.PRODUCT_CATEGORY));
  }

  private Mono<Void> syncProductSpecifications(OverallContext context) {
    return sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_SPECIFICATION, CatalogUtil.getCatalogs(CatalogType.PRODUCT_SPECIFICATION));
  }

  private Mono<Void> syncProductOfferings(OverallContext context) {
    return sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_OFFERING, CatalogUtil.getCatalogs(CatalogType.PRODUCT_OFFERING));
  }

  private Mono<Void> syncBundles(OverallContext context) {
    return sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_BUNDLES, CatalogUtil.getCatalogs(CatalogType.PRODUCT_BUNDLES));
  }

  private Mono<Void> syncResourceSpecifications(OverallContext context) {
    return sync(catalogSyncProperties.getResourceCatalogUrl(), context,
        CatalogType.RESOURCE_SPECIFICATION, CatalogUtil.getCatalogs(CatalogType.RESOURCE_SPECIFICATION));
  }

  private Mono<Void> syncServiceSpecifications(OverallContext context) {
    return sync(catalogSyncProperties.getServiceCatalogUrl(), context,
        CatalogType.SERVICE_SPECIFICATION, CatalogUtil.getCatalogs(CatalogType.SERVICE_SPECIFICATION));
  }

  // ── Core synchronisation logic ────────────────────────────────────────

  private Mono<Void> sync(String baseUrl, OverallContext overall, CatalogType catalogType,
      Resource[] catalogs) {
    return Flux.fromIterable(Arrays.asList(catalogs))
        .flatMap(catalog -> sync1(baseUrl, overall, catalogType, catalog), 1)
        .then();
  }

  private Mono<Void> sync1(String baseUrl, OverallContext overallContext, CatalogType catalogType,
      Resource catalog) {
    SingleContext ctx = new SingleContext();
    ctx.setCatalogType(catalogType);
    ctx.setRequestedCatalog(CatalogUtil.readAsMap(catalog));
    ctx.setId(TypeUtil.asString(ctx.getRequestedCatalog().get("id")));

    URI getUri = uri(baseUrl, resolveGetEndpoint(ctx), ctx.getId());

    return catalogClient.get(getUri)
        .flatMap(body -> handleExistingEntity(baseUrl, overallContext, ctx, body))
        .doOnError(CatalogGetException.class, this::logGetException)
        .onErrorResume(CatalogGetException.class, handleNotFound(baseUrl, overallContext, ctx))
        .doOnError(CatalogPostException.class, e -> log.error("Exception during POST: ", e))
        .doOnError(CatalogPatchException.class, e -> log.error("Exception during PATCH: ", e))
        .then();
  }

  private void logGetException(CatalogGetException e) {
    if (!e.getHttpStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
      log.error("Exception during GET: ", e);
    }
  }

  // ── Existing entity handling ──────────────────────────────────────────

  private Mono<Void> handleExistingEntity(String baseUrl, OverallContext overall,
      SingleContext ctx, String body) {
    if (ctx.getCatalogType().getEntityType() == EntityType.MULTI_VERSIONED) {
      return handleMultiVersionedExisting(baseUrl, overall, ctx, body);
    }
    return patchIfNecessary(baseUrl, overall, ctx, body);
  }

  /**
   * MULTI_VERSIONED entities returned by {@code GET /endpoint/id} carry a version field.
   * <ul>
   *   <li>{@code version == "0"} → only the design version exists; create the launched version.</li>
   *   <li>{@code version > "0"} → a launched version exists; patch it if content differs,
   *       using a versioned URL {@code /endpoint/id:(version=N)}.</li>
   * </ul>
   */
  private Mono<Void> handleMultiVersionedExisting(String baseUrl, OverallContext overall,
      SingleContext ctx, String body) {
    Map<String, Object> existingMap = CatalogUtil.readAsMap(body);
    String existingVersion = TypeUtil.asString(existingMap.get(CatalogConstants.VERSION));

    if ("0".equals(existingVersion)) {
      log.debug("MULTI_VERSIONED {} id={}: only version 0 exists, creating launched version.",
          ctx.getCatalogType(), ctx.getId());
      return createLaunchedVersion(baseUrl, overall, ctx);
    }

    ctx.setExistingVersion(existingVersion);
    return patchIfNecessary(baseUrl, overall, ctx, body);
  }

  private Mono<Void> createLaunchedVersion(String baseUrl, OverallContext overall,
      SingleContext ctx) {
    URI postUri = uri(baseUrl, resolvePostPatchEndpoint(ctx));
    String postBody = CatalogUtil.stripForPost(ctx.getRequestedCatalog());
    String launchedBody = CatalogUtil.launchedVersion(postBody);
    return catalogClient.post(postUri, launchedBody)
        .doOnNext(respBody -> overall.addCreatedCatalog(
            catalog(ctx.getCatalogType(), CatalogUtil.readAsMap(respBody))))
        .then();
  }

  // ── Patch (update) logic ──────────────────────────────────────────────

  private Mono<Void> patchIfNecessary(String baseUrl, OverallContext overall,
      SingleContext ctx, String body) {
    if (!ctx.getCatalogType().isPatchable()) {
      log.debug("Skipping patch checks for {}, id = {}", ctx.getCatalogType(), ctx.getId());
      return Mono.empty();
    }
    ctx.setExistingCatalog(CatalogUtil.readAsMap(body));
    if (CatalogUtil.requestedEqualsExisting(ctx)) {
      log.debug("Skipping patch because {} id = {} is up to date.", ctx.getCatalogType(),
          ctx.getId());
      return Mono.empty();
    }
    log.debug("Will patch {} id = {}", ctx.getCatalogType(), ctx.getId());

    String idSegment = ctx.getId();
    if (ctx.getCatalogType().getEntityType() == EntityType.MULTI_VERSIONED
        && ctx.getExistingVersion() != null) {
      idSegment = ctx.getId() + ":(version=" + ctx.getExistingVersion() + ")";
    }
    URI patchUri = uri(baseUrl, resolvePostPatchEndpoint(ctx), idSegment);
    String patchBody = CatalogUtil.stripForPatch(ctx.getRequestedCatalog());

    return catalogClient.patch(patchUri, ctx.getCatalogType().getPatchType(), patchBody)
        .doOnNext(patchedBody -> overall.addUpdatedCatalog(
            catalog(ctx.getCatalogType(), CatalogUtil.readAsMap(patchedBody))))
        .then();
  }

  // ── Create (POST) logic ───────────────────────────────────────────────

  private java.util.function.Function<? super CatalogGetException, ? extends Mono<Void>>
      handleNotFound(String baseUrl, OverallContext overall, SingleContext ctx) {
    return e -> {
      if (!e.getHttpStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
        log.error("Unexpected status code {} received. Stopping.", e.getHttpStatusCode());
        return Mono.error(e);
      }
      log.debug("Will create {} id = {}", ctx.getCatalogType(), ctx.getId());
      URI postUri = uri(baseUrl, resolvePostPatchEndpoint(ctx));
      return switch (ctx.getCatalogType().getEntityType()) {
        case MULTI_VERSIONED -> createMultiVersioned(postUri, overall, ctx);
        case SINGLE_VERSIONED -> createSingleVersioned(postUri, overall, ctx);
        case NORMAL -> createNormal(postUri, overall, ctx);
      };
    };
  }

  private Mono<Void> createMultiVersioned(URI postUri, OverallContext overall, SingleContext ctx) {
    String postBody = CatalogUtil.stripForPost(ctx.getRequestedCatalog());
    String v0Body = CatalogUtil.version0(postBody);
    String v1Body = CatalogUtil.launchedVersion(postBody);
    return catalogClient.post(postUri, v0Body)
        .then(catalogClient.post(postUri, v1Body))
        .doOnNext(respBody -> overall.addCreatedCatalog(
            catalog(ctx.getCatalogType(), CatalogUtil.readAsMap(respBody))))
        .then();
  }

  private Mono<Void> createSingleVersioned(URI postUri, OverallContext overall, SingleContext ctx) {
    String postBody = CatalogUtil.stripForPost(ctx.getRequestedCatalog());
    String v1Body = CatalogUtil.launchedVersion(postBody);
    return catalogClient.post(postUri, v1Body)
        .doOnNext(respBody -> overall.addCreatedCatalog(
            catalog(ctx.getCatalogType(), CatalogUtil.readAsMap(respBody))))
        .then();
  }

  private Mono<Void> createNormal(URI postUri, OverallContext overall, SingleContext ctx) {
    String postBody = CatalogUtil.stripForPost(ctx.getRequestedCatalog());
    return catalogClient.post(postUri, postBody)
        .doOnNext(respBody -> overall.addCreatedCatalog(
            catalog(ctx.getCatalogType(), CatalogUtil.readAsMap(respBody))))
        .then();
  }

  // ── Endpoint resolution helpers ───────────────────────────────────────

  private String resolveGetEndpoint(SingleContext ctx) {
    CatalogType type = ctx.getCatalogType();
    if (type != CatalogType.RESOURCE_SPECIFICATION) {
      return type.getGetEndpoint();
    }
    Map<String, Object> json = loadJson(type, ctx.getId());
    return EndpointResolver.getGetEndpoint(type, json);
  }

  private String resolvePostPatchEndpoint(SingleContext ctx) {
    CatalogType type = ctx.getCatalogType();
    if (type != CatalogType.RESOURCE_SPECIFICATION) {
      return type.getPostPatchEndpoint();
    }
    Map<String, Object> json = loadJson(type, ctx.getId());
    return EndpointResolver.getPostPatchEndpoint(type, json);
  }

  private Map<String, Object> loadJson(CatalogType catalogType, String id) {
    var path = "catalog/" + catalogType.getLocationPattern() + "/" + id + ".json";
    return JacksonUtil.jsonToMap(JacksonUtil.contents(path));
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  private Catalog catalog(CatalogType type, Map<String, Object> map) {
    return new Catalog(type,
        TypeUtil.asString(map.get("id")),
        TypeUtil.asString(map.get("name")),
        TypeUtil.asString(map.get("version")),
        TypeUtil.asLong(map.get("revision")));
  }

  private void releaseLock(AcquiredLock lock, int deployedCount) {
    try {
      dbLockService.releaseLock(lock, deployedCount > 0);
    } catch (DbLockException e) {
      throw new IllegalStateException("Unexpected error during lock release.", e);
    }
  }
}
