package org.opentmf.catalog.sync.config;

import jakarta.validation.constraints.NotEmpty;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author Gokhan Demir
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "opentmf.catalog-sync")
public class CatalogSyncProperties {

  private boolean enabled = true;

  /**
   * The current deployment version of the catalog files of the calling project.
   * Provide a higher version even if only one catalog definition changes.
   * <p>
   *   Please note: The version comparison is performed by pure string comparison
   *   (using String.compareTo). For example version 1.9 is greater than version 1.10.
   * </p>
   */
  @NotEmpty
  private String catalogVersion;

  /**
   * Minimum time that must have passed since the last release before a downgrade is allowed.
   */
  private Duration downgradeAllowedAfter = Duration.ofMinutes(10);

  private String productCatalogUrl;

  private String resourceCatalogUrl;

  private String serviceCatalogUrl;

  /**
   * Reference to the HTTP client configured under {@code opentmf.http-clients.<client-ref>}.
   * The following beans are resolved by this prefix:
   * <ul>
   *   <li>{@code <clientRef>WebClient} or {@code <clientRef>RestClient}</li>
   *   <li>{@code <clientRef>TokenService}</li>
   *   <li>{@code <clientRef>ClientProperties}</li>
   * </ul>
   */
  @NotEmpty
  private String clientRef;
}
