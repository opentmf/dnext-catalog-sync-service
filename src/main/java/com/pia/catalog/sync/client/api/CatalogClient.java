package com.pia.catalog.sync.client.api;

import com.pia.catalog.sync.model.SingleContext;
import reactor.core.publisher.Mono;

/**
 * @author Gokhan Demir
 */
public interface CatalogClient {

  Mono<String> get(String baseUrl, SingleContext context);

  Mono<String> post(String baseUrl, SingleContext context);

  Mono<String> patch(String baseUrl, SingleContext context);
}
