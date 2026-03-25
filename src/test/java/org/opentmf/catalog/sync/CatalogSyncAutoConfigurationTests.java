package org.opentmf.catalog.sync;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentmf.catalog.sync.config.CatalogSyncAutoConfiguration;
import org.opentmf.catalog.sync.config.CatalogSyncProperties;
import org.opentmf.db.lock.service.api.DbLockService;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

/**
 * @author Gokhan Demir
 */
class CatalogSyncAutoConfigurationTests {

  @Test
  void testCatalogSyncAutoConfiguration_withMissingBeans_throwsException() {
    var ctx = Mockito.mock(ApplicationContext.class);
    Mockito.when(ctx.getBean(Mockito.anyString()))
        .thenThrow(new NoSuchBeanDefinitionException("test"));
    Assertions.assertThrows(NoSuchBeanDefinitionException.class, () ->
        new CatalogSyncAutoConfiguration(
            ctx,
            Mockito.mock(DbLockService.class),
            Mockito.mock(CatalogSyncProperties.class)));
  }
}
