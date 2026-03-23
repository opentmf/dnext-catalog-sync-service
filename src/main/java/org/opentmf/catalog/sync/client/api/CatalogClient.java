package org.opentmf.catalog.sync.client.api;

import java.net.URI;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

/**
 * Abstraction over the HTTP transport layer. Both reactive (WebClient) and synchronous
 * (RestTemplate) implementations wrap their results in {@link Mono} so that the service
 * layer can use a single reactive pipeline.
 *
 * @author Gokhan Demir
 */
public interface CatalogClient {

  Mono<String> get(URI uri);

  Mono<String> post(URI uri, String body);

  Mono<String> patch(URI uri, MediaType patchType, String body);
}
