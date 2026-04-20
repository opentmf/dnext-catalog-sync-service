package org.opentmf.catalog.sync.service.api;

import org.opentmf.db.lock.model.LockContext;

/**
 * @author Gokhan Demir
 */
public interface CatalogSyncService {

  void ensureCatalogConsistency(LockContext lockContext);
}
