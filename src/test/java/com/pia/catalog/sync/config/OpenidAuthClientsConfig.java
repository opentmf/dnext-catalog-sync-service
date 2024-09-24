package com.pia.catalog.sync.config;

import com.pia.client.openid.model.OpenidClientProperties;
import com.pia.client.openid.model.OpenidClients;
import com.pia.client.openid.service.api.OpenidTokenService;
import com.pia.client.openid.service.api.OpenidWebClientProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Gokhan Demir
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OpenidClients.class)
public class OpenidAuthClientsConfig {

  private final OpenidWebClientProvider openidWebClientProvider;
  private final OpenidClients openidClients;

  @Bean
  public OpenidClientProperties defaultClientProperties() {
    return openidClients.getOpenid().get("default");
  }

  @Bean
  public WebClient defaultWebClient(OpenidClientProperties properties) {
    return openidWebClientProvider.buildWebClient(properties);
  }

  @Bean
  public OpenidTokenService defaultTokenService(OpenidClientProperties properties) {
    return openidWebClientProvider.buildTokenService(properties);
  }
}
