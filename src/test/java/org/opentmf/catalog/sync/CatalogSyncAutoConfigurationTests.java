package org.opentmf.catalog.sync;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentmf.catalog.sync.config.CatalogSyncAutoConfiguration;
import org.opentmf.catalog.sync.config.CatalogSyncProperties;
import org.opentmf.db.lock.service.api.DbLockService;
import org.springframework.context.ApplicationContext;

/**
 * @author Gokhan Demir
 */
class CatalogSyncAutoConfigurationTests {

  @Test
  void testCatalogSyncAutoConfiguration_withInvalidData_throwsException() {
    Assertions.assertThrows(Exception.class, () ->
        new CatalogSyncAutoConfiguration(
            Mockito.mock(ApplicationContext.class),
            Mockito.mock(DbLockService.class),
            Mockito.mock(CatalogSyncProperties.class)));
  }
}
