package org.opentmf.catalog.sync.util;

import lombok.Generated;

/**
 * @author Gokhan Demir
 */
public final class TypeUtil {

  @Generated
  private TypeUtil() {
  }

  public static String asString(Object obj) {
    if (obj == null) {
      return null;
    }
    return obj instanceof String aString ? aString : obj.toString();
  }

  public static Long asLong(Object obj) {
    if (obj == null) {
      return null;
    }
    return obj instanceof Long aLong ? aLong : Long.valueOf(obj.toString());
  }
}
