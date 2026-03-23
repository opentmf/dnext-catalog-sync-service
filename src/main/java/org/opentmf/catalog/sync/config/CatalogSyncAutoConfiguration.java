package org.opentmf.catalog.sync.config;

import lombok.extern.slf4j.Slf4j;
import org.opentmf.catalog.sync.client.api.CatalogClient;
import org.opentmf.catalog.sync.client.impl.ReactiveCatalogClientImpl;
import org.opentmf.catalog.sync.client.impl.RestCatalogClientImpl;
import org.opentmf.catalog.sync.service.impl.CatalogSyncServiceImpl;
import org.opentmf.client.common.model.ClientProperties;
import org.opentmf.client.reactive.service.api.TokenService;
import org.opentmf.client.rest.service.api.SyncTokenService;
import org.opentmf.db.lock.config.DbLockAutoConfiguration;
import org.opentmf.db.lock.service.api.DbLockService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration that initialises the catalog sync service on application startup.
 * Detects whether the configured client-ref points to a reactive (WebClient) or
 * synchronous (RestTemplate) HTTP client and creates the appropriate {@link CatalogClient}.
 *
 * @author Gokhan Demir
 */
@AutoConfiguration(
    after = DbLockAutoConfiguration.class,
    afterName = "org.opentmf.client.starter.OpentmfHttpClientsAutoConfiguration")
@ConditionalOnBean(name = "dbLockService")
@EnableConfigurationProperties({CatalogSyncProperties.class})
@ConditionalOnProperty(
    prefix = "opentmf.catalog-sync",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
public class CatalogSyncAutoConfiguration {

  public CatalogSyncAutoConfiguration(ApplicationContext ctx, DbLockService dbLockService,
      CatalogSyncProperties catalogSyncProperties) {
    log.info("Initializing the Catalog Sync Service.");
    var clientRef = catalogSyncProperties.getClientRef();
    var clientProperties = (ClientProperties) ctx.getBean(clientRef + "ClientProperties");
    var catalogClient = buildCatalogClient(ctx, clientRef, clientProperties);
    var catalogSyncService = new CatalogSyncServiceImpl(catalogSyncProperties, dbLockService,
        catalogClient);
    catalogSyncService.ensureCatalogConsistency();
    log.info("Completed initializing the Catalog Sync Service.");
  }

  private CatalogClient buildCatalogClient(ApplicationContext ctx, String clientRef,
      ClientProperties clientProperties) {
    if (ctx.containsBean(clientRef + "WebClient")) {
      log.info("Using reactive (WebClient) transport for client-ref '{}'.", clientRef);
      var webClient = (WebClient) ctx.getBean(clientRef + "WebClient");
      var tokenService = (TokenService) ctx.getBean(clientRef + "TokenService");
      return new ReactiveCatalogClientImpl(webClient, tokenService, clientProperties);
    }
    log.info("Using synchronous (RestTemplate) transport for client-ref '{}'.", clientRef);
    var restTemplate = (RestTemplate) ctx.getBean(clientRef + "RestTemplate");
    var syncTokenService = (SyncTokenService) ctx.getBean(clientRef + "TokenService");
    return new RestCatalogClientImpl(restTemplate, syncTokenService, clientProperties);
  }
}
