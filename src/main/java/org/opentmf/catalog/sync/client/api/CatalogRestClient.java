package org.opentmf.catalog.sync.client.api;

import java.net.URI;
import org.springframework.http.MediaType;

/**
 * Synchronous HTTP transport abstraction backed by {@link org.springframework.web.client.RestClient}.
 *
 * @author Gokhan Demir
 */
public interface CatalogRestClient {

  String get(URI uri);

  String post(URI uri, String body);

  String patch(URI uri, MediaType patchType, String body);
}
