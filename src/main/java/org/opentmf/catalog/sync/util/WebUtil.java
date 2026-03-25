package org.opentmf.catalog.sync.util;

import java.net.URI;
import java.util.Arrays;
import lombok.Generated;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Gokhan Demir
 */
public final class WebUtil {

  @Generated
  private WebUtil() {
  }

  public static URI uri(String path, String... segment) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(path);
    Arrays.stream(segment).forEach(builder::pathSegment);
    return builder.build().toUri();
  }
}
