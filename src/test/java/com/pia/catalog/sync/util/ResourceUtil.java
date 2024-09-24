package com.pia.catalog.sync.util;

import static com.pia.commons.util.JacksonUtil.contents;

import java.io.IOException;
import org.springframework.core.io.Resource;

/**
 * @author Gokhan Demir
 */
public final class ResourceUtil {

  private ResourceUtil() {
  }

  public static String readAsString(Resource json) {
    try {
      return contents(json.getInputStream());
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
