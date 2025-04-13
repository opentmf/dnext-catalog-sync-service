package org.opentmf.catalog.sync.config;

import jakarta.validation.constraints.NotEmpty;
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

  /**
   * If false, no synchronization will be attempted.
   */
  private boolean enabled = true;

  /**
   * The current deployment version of the catalog files of the calling project.
   * Provide a higher version even if only one catalog definition changes.
   * <strong>Mandatory</strong>.
   * <p>
   *   Please note:
   *   <ul>
   *     <li>The version comparison is performed by pure string comparison (using String.compareTo
   *     method). For example version 1.9 is greater than version 1.10 in pure string comparison.
   *     Please provide version numbers accordingly to get rid of unwanted situations.</li>
   *   </ul>
   * </p>
   */
  @NotEmpty
  private String catalogVersion;

  /**
   * In terms of milliseconds, specifies the minimum time required for a downgrade decision to be
   * made, when the requested bpmnVersion is older than the last synchronized version.
   */
  private long downgradeAllowedAfter = 600000L;

  /**
   * Product Catalog URL.
   */
  private String productCatalogUrl;

  /**
   * Resource Catalog URL
   */
  private String resourceCatalogUrl;

  /**
   * Service Catalog URL
   */
  private String serviceCatalogUrl;

  /**
   * The client id to use. This id is the prefix to the following exposed beans:
   * <ul>
   *   <li>webClient</li>
   *   <li>tokenService</li>
   *   <li>clientProperties</li>
   * </ul>
   * <p>
   *   For example, if the client value is <strong>sample</strong> then we will assume the
   *   following three beans are exposed:
   *   <ul>
   *     <li>(WebClient) sampleWebClient</li>
   *     <li>(TokenService) sampleTokenService</li>
   *     <li>(BaseClientProperties) sampleClientProperties</li>
   *   </ul>
   * </p>
   */
  @NotEmpty
  private String client;
}
