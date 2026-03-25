package org.opentmf.catalog.sync.config;

import lombok.extern.slf4j.Slf4j;
import org.opentmf.catalog.sync.client.impl.CatalogReactiveClientImpl;
import org.opentmf.catalog.sync.client.impl.CatalogRestClientImpl;
import org.opentmf.catalog.sync.service.api.CatalogSyncService;
import org.opentmf.catalog.sync.service.impl.ReactiveCatalogSyncServiceImpl;
import org.opentmf.catalog.sync.service.impl.RestCatalogSyncServiceImpl;
import org.opentmf.client.common.model.ClientProperties;
import org.opentmf.client.reactive.service.api.TokenService;
import org.opentmf.client.rest.service.api.SyncTokenService;
import org.opentmf.db.lock.config.DbLockAutoConfiguration;
import org.opentmf.db.lock.service.api.DbLockService;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration that initialises the catalog sync service on application startup.
 * Detects whether the configured client-ref points to a reactive (WebClient) or
 * synchronous (RestTemplate) HTTP client and creates the appropriate service implementation.
 *
 * @author Gokhan Demir
 */
@AutoConfiguration(
    after = DbLockAutoConfiguration.class,
    afterName = "org.opentmf.client.starter.OpentmfHttpClientsAutoConfiguration")
@ConditionalOnBean(name = "dbLockService")
@EnableConfigurationProperties({CatalogSyncProperties.class})
@ConditionalOnProperty(prefix = "opentmf.catalog-sync", name = "catalog-version")
@Slf4j
public class CatalogSyncAutoConfiguration implements SmartInitializingSingleton {

  private final CatalogSyncProperties catalogSyncProperties;
  private final CatalogSyncService catalogSyncService;

  public CatalogSyncAutoConfiguration(ApplicationContext ctx, DbLockService dbLockService,
      CatalogSyncProperties catalogSyncProperties) {
    this.catalogSyncProperties = catalogSyncProperties;
    var clientRef = catalogSyncProperties.getClientRef();
    var clientProperties = (ClientProperties) ctx.getBean(clientRef + "ClientProperties");
    this.catalogSyncService = buildSyncService(ctx, clientRef, clientProperties, dbLockService,
        catalogSyncProperties);
  }

  @Bean
  public CatalogSyncService catalogSyncService() {
    return catalogSyncService;
  }

  @Override
  public void afterSingletonsInstantiated() {
    if (catalogSyncProperties.isEnabled()) {
      log.info("Initializing the Catalog Sync Service.");
      try {
        catalogSyncService.ensureCatalogConsistency();
      } finally {
        log.info("Completed initializing the Catalog Sync Service.");
      }
    }
  }

  private static CatalogSyncService buildSyncService(ApplicationContext ctx, String clientRef,
      ClientProperties clientProperties, DbLockService dbLockService,
      CatalogSyncProperties catalogSyncProperties) {
    if (ctx.containsBean(clientRef + "WebClient")) {
      log.info("Using reactive (WebClient) transport for client-ref '{}'.", clientRef);
      var webClient = (WebClient) ctx.getBean(clientRef + "WebClient");
      var tokenService = (TokenService) ctx.getBean(clientRef + "TokenService");
      var client = new CatalogReactiveClientImpl(webClient, tokenService, clientProperties);
      return new ReactiveCatalogSyncServiceImpl(catalogSyncProperties, dbLockService, client);
    }
    log.info("Using synchronous (RestClient) transport for client-ref '{}'.", clientRef);
    var restClient = (RestClient) ctx.getBean(clientRef + "RestClient");
    var syncTokenService = (SyncTokenService) ctx.getBean(clientRef + "TokenService");
    var client = new CatalogRestClientImpl(restClient, syncTokenService, clientProperties);
    return new RestCatalogSyncServiceImpl(catalogSyncProperties, dbLockService, client);
  }
}
