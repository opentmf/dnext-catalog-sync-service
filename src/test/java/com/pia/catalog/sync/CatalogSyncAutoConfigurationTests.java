package com.pia.catalog.sync;

import com.pia.catalog.sync.config.CatalogSyncAutoConfiguration;
import com.pia.catalog.sync.config.CatalogSyncProperties;
import com.pia.db.lock.service.api.DbLockService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

/**
 * @author Gokhan Demir
 */
class CatalogSyncAutoConfigurationTests {

  @Test
  void testCatalogSyncAutoConfiguration_withInvalidData_throwsException() {
    // for getting rid of unused class.
    Assertions.assertThrows(Exception.class, () ->
        new CatalogSyncAutoConfiguration(
            Mockito.mock(ApplicationContext.class),
            Mockito.mock(DbLockService.class),
            Mockito.mock(CatalogSyncProperties.class)));
  }
}
