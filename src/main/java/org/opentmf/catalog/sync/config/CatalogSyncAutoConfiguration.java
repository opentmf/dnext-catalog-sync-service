package org.opentmf.catalog.sync.config;

import org.opentmf.catalog.sync.client.impl.CatalogClientImpl;
import org.opentmf.catalog.sync.service.impl.CatalogSyncServiceImpl;
import org.opentmf.client.common.model.BaseClientProperties;
import org.opentmf.client.common.service.api.TokenService;
import org.opentmf.db.lock.config.DbLockAutoConfiguration;
import org.opentmf.db.lock.service.api.DbLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Gokhan Demir
 */
@AutoConfiguration(
    after = DbLockAutoConfiguration.class,
    afterName = {
        "org.opentmf.client.openid.config.OpenidWebClientProviderAutoConfiguration",
        "org.opentmf.client.openid.config.OpenidWebClientsStarterAutoConfiguration",
        "org.opentmf.client.basic.config.BasicWebClientProviderAutoConfiguration",
        "org.opentmf.client.basic.config.BasicWebClientsStarterAutoConfiguration"
    })
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
    var client = catalogSyncProperties.getClient();
    var webClient = (WebClient) ctx.getBean(client + "WebClient");
    var tokenService = (TokenService) ctx.getBean(client + "TokenService");
    var clientProperties = (BaseClientProperties) ctx.getBean(client + "ClientProperties");
    var catalogClient = new CatalogClientImpl(webClient, tokenService, clientProperties);
    var catalogSyncService = new CatalogSyncServiceImpl(catalogSyncProperties, dbLockService, catalogClient);

    // do real task
    catalogSyncService.ensureCatalogConsistency();
    log.info("Completed initializing the Catalog Sync Service.");
  }
}
