package org.opentmf.catalog.sync.service.impl;

import static org.opentmf.catalog.sync.util.WebUtil.uri;

import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opentmf.catalog.sync.client.api.CatalogRestClient;
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

/**
 * Synchronous {@link CatalogSyncService} implementation backed by {@link CatalogRestClient}.
 *
 * @author Gokhan Demir
 */
@Slf4j
@RequiredArgsConstructor
public class RestCatalogSyncServiceImpl implements CatalogSyncService {

  private static final String PRODUCT = "product";
  public static final String RESOURCE = "resource";
  public static final String SERVICE = "service";

  private final CatalogSyncProperties catalogSyncProperties;
  private final DbLockService dbLockService;
  private final CatalogRestClient catalogClient;

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
    syncResourceSpecifications(context);
    syncServiceSpecifications(context);
    syncProductCategories(context);
    syncProductCatalogs(context);
    syncProductSpecifications(context);
    syncProductOfferingPrices(context);
    syncProductOfferings(context);
    syncBundles(context);
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

  private void syncProductCategories(OverallContext context) {
    sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_CATEGORY, CatalogUtil.getCatalogs(CatalogType.PRODUCT_CATEGORY));
  }

  private void syncProductCatalogs(OverallContext context) {
    sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_CATALOG, CatalogUtil.getCatalogs(CatalogType.PRODUCT_CATALOG));
  }

  private void syncProductSpecifications(OverallContext context) {
    sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_SPECIFICATION, CatalogUtil.getCatalogs(CatalogType.PRODUCT_SPECIFICATION));
  }

  private void syncProductOfferingPrices(OverallContext context) {
    sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_OFFERING_PRICE, CatalogUtil.getCatalogs(CatalogType.PRODUCT_OFFERING_PRICE));
  }

  private void syncProductOfferings(OverallContext context) {
    sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_OFFERING, CatalogUtil.getCatalogs(CatalogType.PRODUCT_OFFERING));
  }

  private void syncBundles(OverallContext context) {
    sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_BUNDLES, CatalogUtil.getCatalogs(CatalogType.PRODUCT_BUNDLES));
  }

  private void syncResourceSpecifications(OverallContext context) {
    sync(catalogSyncProperties.getResourceCatalogUrl(), context,
        CatalogType.RESOURCE_SPECIFICATION, CatalogUtil.getCatalogs(CatalogType.RESOURCE_SPECIFICATION));
  }

  private void syncServiceSpecifications(OverallContext context) {
    sync(catalogSyncProperties.getServiceCatalogUrl(), context,
        CatalogType.SERVICE_SPECIFICATION, CatalogUtil.getCatalogs(CatalogType.SERVICE_SPECIFICATION));
  }

  // ── Core synchronisation logic ────────────────────────────────────────

  private void sync(String baseUrl, OverallContext overall, CatalogType catalogType,
      Resource[] catalogs) {
    for (Resource catalog : catalogs) {
      sync1(baseUrl, overall, catalogType, catalog);
    }
  }

  private void sync1(String baseUrl, OverallContext overallContext, CatalogType catalogType,
      Resource catalog) {
    SingleContext ctx = new SingleContext();
    ctx.setCatalogType(catalogType);
    ctx.setRequestedCatalog(CatalogUtil.readAsMap(catalog));
    ctx.setId(TypeUtil.asString(ctx.getRequestedCatalog().get("id")));

    URI getUri = uri(baseUrl, resolveGetEndpoint(ctx), ctx.getId());

    try {
      String body = catalogClient.get(getUri);
      handleExistingEntity(baseUrl, overallContext, ctx, body);
    } catch (CatalogGetException e) {
      handleGetException(baseUrl, overallContext, ctx, e);
    } catch (CatalogPostException e) {
      log.error("Exception during POST: ", e);
      throw e;
    } catch (CatalogPatchException e) {
      log.error("Exception during PATCH: ", e);
      throw e;
    }
  }

  private void handleGetException(String baseUrl, OverallContext overall, SingleContext ctx,
      CatalogGetException e) {
    if (!e.getHttpStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
      log.error("Exception during GET: ", e);
      log.error("Unexpected status code {} received. Stopping.", e.getHttpStatusCode());
      throw e;
    }
    log.debug("Will create {} id = {}", ctx.getCatalogType(), ctx.getId());
    URI postUri = uri(baseUrl, resolvePostPatchEndpoint(ctx));
    switch (ctx.getCatalogType().getEntityType()) {
      case MULTI_VERSIONED -> createMultiVersioned(postUri, overall, ctx);
      case SINGLE_VERSIONED -> createSingleVersioned(postUri, overall, ctx);
      case NORMAL -> createNormal(postUri, overall, ctx);
    }
  }

  // ── Existing entity handling ──────────────────────────────────────────

  private void handleExistingEntity(String baseUrl, OverallContext overall,
      SingleContext ctx, String body) {
    if (ctx.getCatalogType().getEntityType() == EntityType.MULTI_VERSIONED) {
      handleMultiVersionedExisting(baseUrl, overall, ctx, body);
      return;
    }
    patchIfNecessary(baseUrl, overall, ctx, body);
  }

  private void handleMultiVersionedExisting(String baseUrl, OverallContext overall,
      SingleContext ctx, String body) {
    Map<String, Object> existingMap = CatalogUtil.readAsMap(body);
    String existingVersion = TypeUtil.asString(existingMap.get(CatalogConstants.VERSION));

    if ("0".equals(existingVersion)) {
      log.debug("MULTI_VERSIONED {} id={}: only version 0 exists, creating launched version.",
          ctx.getCatalogType(), ctx.getId());
      createLaunchedVersion(baseUrl, overall, ctx);
      return;
    }

    ctx.setExistingVersion(existingVersion);
    patchIfNecessary(baseUrl, overall, ctx, body);
  }

  private void createLaunchedVersion(String baseUrl, OverallContext overall,
      SingleContext ctx) {
    URI postUri = uri(baseUrl, resolvePostPatchEndpoint(ctx));
    String postBody = CatalogUtil.stripForPost(ctx.getRequestedCatalog());
    String launchedBody = CatalogUtil.launchedVersion(postBody);
    String respBody = catalogClient.post(postUri, launchedBody);
    overall.addCreatedCatalog(catalog(ctx.getCatalogType(), CatalogUtil.readAsMap(respBody)));
  }

  // ── Patch (update) logic ──────────────────────────────────────────────

  private void patchIfNecessary(String baseUrl, OverallContext overall,
      SingleContext ctx, String body) {
    if (!ctx.getCatalogType().isPatchable()) {
      log.debug("Skipping patch checks for {}, id = {}", ctx.getCatalogType(), ctx.getId());
      return;
    }
    ctx.setExistingCatalog(CatalogUtil.readAsMap(body));
    if (CatalogUtil.requestedEqualsExisting(ctx)) {
      log.debug("Skipping patch because {} id = {} is up to date.", ctx.getCatalogType(),
          ctx.getId());
      return;
    }
    log.debug("Will patch {} id = {}", ctx.getCatalogType(), ctx.getId());

    String idSegment = ctx.getId();
    if (ctx.getCatalogType().getEntityType() == EntityType.MULTI_VERSIONED
        && ctx.getExistingVersion() != null) {
      idSegment = ctx.getId() + ":(version=" + ctx.getExistingVersion() + ")";
    }
    URI patchUri = uri(baseUrl, resolvePostPatchEndpoint(ctx), idSegment);
    String patchBody = CatalogUtil.stripForPatch(ctx.getRequestedCatalog());

    String patchedBody = catalogClient.patch(patchUri, ctx.getCatalogType().getPatchType(), patchBody);
    overall.addUpdatedCatalog(catalog(ctx.getCatalogType(), CatalogUtil.readAsMap(patchedBody)));
  }

  // ── Create (POST) logic ───────────────────────────────────────────────

  private void createMultiVersioned(URI postUri, OverallContext overall, SingleContext ctx) {
    String postBody = CatalogUtil.stripForPost(ctx.getRequestedCatalog());
    String v0Body = CatalogUtil.version0(postBody);
    String v1Body = CatalogUtil.launchedVersion(postBody);
    catalogClient.post(postUri, v0Body);
    String respBody = catalogClient.post(postUri, v1Body);
    overall.addCreatedCatalog(catalog(ctx.getCatalogType(), CatalogUtil.readAsMap(respBody)));
  }

  private void createSingleVersioned(URI postUri, OverallContext overall, SingleContext ctx) {
    String postBody = CatalogUtil.stripForPost(ctx.getRequestedCatalog());
    String v1Body = CatalogUtil.launchedVersion(postBody);
    String respBody = catalogClient.post(postUri, v1Body);
    overall.addCreatedCatalog(catalog(ctx.getCatalogType(), CatalogUtil.readAsMap(respBody)));
  }

  private void createNormal(URI postUri, OverallContext overall, SingleContext ctx) {
    String postBody = CatalogUtil.stripForPost(ctx.getRequestedCatalog());
    String respBody = catalogClient.post(postUri, postBody);
    overall.addCreatedCatalog(catalog(ctx.getCatalogType(), CatalogUtil.readAsMap(respBody)));
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
