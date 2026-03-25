package org.opentmf.catalog.sync.client.api;

import java.net.URI;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

/**
 * Reactive HTTP transport abstraction backed by {@link org.springframework.web.reactive.function.client.WebClient}.
 *
 * @author Gokhan Demir
 */
public interface CatalogReactiveClient {

  Mono<String> get(URI uri);

  Mono<String> post(URI uri, String body);

  Mono<String> patch(URI uri, MediaType patchType, String body);
}
