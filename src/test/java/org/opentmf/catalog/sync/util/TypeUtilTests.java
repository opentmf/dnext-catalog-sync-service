package org.opentmf.catalog.sync.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 */
class TypeUtilTests {

  @Test
  void testAsString() {
    Assertions.assertNull(TypeUtil.asString(null));
    Assertions.assertEquals("4", TypeUtil.asString(4L));
    Assertions.assertEquals("4", TypeUtil.asString("4"));
  }

  @Test
  void testAsLong() {
    Assertions.assertNull(TypeUtil.asLong(null));
    Assertions.assertEquals(4L, TypeUtil.asLong(4L));
    Assertions.assertEquals(4L, TypeUtil.asLong("4"));
  }
}
