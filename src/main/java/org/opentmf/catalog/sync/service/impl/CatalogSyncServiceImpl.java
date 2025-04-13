package org.opentmf.catalog.sync.service.impl;


import org.opentmf.catalog.sync.client.api.CatalogClient;
import org.opentmf.catalog.sync.config.CatalogSyncProperties;
import org.opentmf.catalog.sync.exception.CatalogGetException;
import org.opentmf.catalog.sync.exception.CatalogPatchException;
import org.opentmf.catalog.sync.exception.CatalogPostException;
import org.opentmf.catalog.sync.model.Catalog;
import org.opentmf.catalog.sync.model.CatalogType;
import org.opentmf.catalog.sync.model.OverallContext;
import org.opentmf.catalog.sync.model.SingleContext;
import org.opentmf.catalog.sync.service.api.CatalogSyncService;
import org.opentmf.catalog.sync.util.CatalogUtil;
import org.opentmf.catalog.sync.util.JacksonUtil2;
import org.opentmf.catalog.sync.util.TypeUtil;
import org.opentmf.commons.util.UrlUtil;
import org.opentmf.db.lock.exception.DbLockException;
import org.opentmf.db.lock.model.AcquiredLock;
import org.opentmf.db.lock.model.LockType;
import org.opentmf.db.lock.service.api.DbLockService;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
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

  private Mono<Void> syncProductCategories(OverallContext context) {
    Resource[] catalogs = CatalogUtil.getCatalogs(CatalogType.PRODUCT_CATEGORY);
    return sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_CATEGORY, catalogs);
  }

  private Mono<Void> syncProductSpecifications(OverallContext context) {
    Resource[] catalogs = CatalogUtil.getCatalogs(CatalogType.PRODUCT_SPECIFICATION);
    return sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_SPECIFICATION, catalogs);
  }

  private Mono<Void> syncProductOfferings(OverallContext context) {
    Resource[] catalogs = CatalogUtil.getCatalogs(CatalogType.PRODUCT_OFFERING);
    return sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_OFFERING, catalogs);
  }

  private Mono<Void> syncBundles(OverallContext context) {
    Resource[] catalogs = CatalogUtil.getCatalogs(CatalogType.PRODUCT_BUNDLES);
    return sync(catalogSyncProperties.getProductCatalogUrl(), context,
        CatalogType.PRODUCT_BUNDLES, catalogs);
  }

  private Mono<Void> syncResourceSpecifications(OverallContext context) {
    Resource[] catalogs = CatalogUtil.getCatalogs(CatalogType.RESOURCE_SPECIFICATION);
    return sync(catalogSyncProperties.getResourceCatalogUrl(), context,
        CatalogType.RESOURCE_SPECIFICATION, catalogs);
  }

  private Mono<Void> syncServiceSpecifications(OverallContext context) {
    Resource[] catalogs = CatalogUtil.getCatalogs(CatalogType.SERVICE_SPECIFICATION);
    return sync(catalogSyncProperties.getServiceCatalogUrl(), context, CatalogType.SERVICE_SPECIFICATION,
            catalogs);
  }

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
    ctx.setRequestedCatalog(JacksonUtil2.readAsMap(catalog));
    ctx.setId(TypeUtil.asString(ctx.getRequestedCatalog().get("id")));

    return catalogClient.get(baseUrl, ctx)
        .flatMap(body -> patchIfNecessary(baseUrl, overallContext, ctx, body))
        .doOnError(CatalogGetException.class, this::logGetException)
        .onErrorResume(CatalogGetException.class, createIfNotExists(baseUrl, overallContext, ctx))
        .doOnError(CatalogPostException.class, e -> log.error("Exception during POST: ", e))
        .doOnError(CatalogPatchException.class, e -> log.error("Exception during PATCH: ", e))
        .then();
  }

  private void logGetException(CatalogGetException e) {
    if (!e.getHttpStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
      log.error("Exception during GET: ", e);
    }
  }

  private Mono<Void> patchIfNecessary(String baseUrl, OverallContext overall,
      SingleContext ctx, String body) {
    if (!ctx.getCatalogType().isPatchable()) {
      log.debug("Skipping patch checks for {}, id = {}", ctx.getCatalogType(), ctx.getId());
      return Mono.empty();
    }
    ctx.setExistingCatalog(JacksonUtil2.readAsMap(body));
    if (CatalogUtil.requestedEqualsExisting(ctx)) {
      log.debug("Skipping patch because {} id = {} is up to date.", ctx.getCatalogType(),
          ctx.getId());
      return Mono.empty();
    }
    log.debug("Will patch {} id = {}", ctx.getCatalogType(), ctx.getId());
    return catalogClient.patch(baseUrl, ctx)
        .doOnNext(patchedBody -> overall.addUpdatedCatalog(
            catalog(ctx.getCatalogType(), JacksonUtil2.readAsMap(patchedBody))))
        .then();
  }

  @NonNull
  private Function<? super CatalogGetException, ? extends Mono<Void>> createIfNotExists(
      String baseUrl, OverallContext overall, SingleContext ctx) {
    return e -> {
      if (e.getHttpStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
        log.debug("Will create {} id = {}", ctx.getCatalogType(), ctx.getId());
        return catalogClient.post(baseUrl, ctx)
            .doOnNext(body -> overall.addCreatedCatalog(
                catalog(ctx.getCatalogType(), JacksonUtil2.readAsMap(body))))
            .then();
      } else {
        log.error("Unexpected status code {} received. Stopping.", e.getHttpStatusCode());
        return Mono.error(e);
      }
    };
  }

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
