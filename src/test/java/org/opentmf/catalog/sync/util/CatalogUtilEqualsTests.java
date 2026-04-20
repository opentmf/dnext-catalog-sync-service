package org.opentmf.catalog.sync.util;

import static org.opentmf.catalog.sync.util.CatalogUtil.stripForPatch;
import static org.opentmf.commons.util.JacksonUtil.contents;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the JSON equality comparison logic in {@link CatalogUtil}.
 *
 * @author Gokhan Demir
 */
class CatalogUtilEqualsTests {

  @Test
  void testEquals_withEqualObjectsButDifferentCharacteristicOrder_returnsTrue() {
    String json1 = contents("json/resource_1.json");
    String json2 = contents("json/resource_2.json");
    Assertions.assertTrue(CatalogUtil.equals("id", json1, json2));

    String json3 = contents("json/resource_3.json");
    Assertions.assertTrue(CatalogUtil.equals("id", json1, json3));

    String json4 = contents("json/resource_4.json");
    Assertions.assertTrue(CatalogUtil.equals("id", json1, json4));
  }

  @Test
  void testEquals_withNonEqualObjects_returnsFalse() {
    String json1 = contents("json/resource_1.json");
    String json5 = contents("json/resource_5.json");
    Assertions.assertFalse(CatalogUtil.equals("id", json1, json5));

    String json6 = contents("json/resource_6.json");
    Assertions.assertFalse(CatalogUtil.equals("id", json1, json6));

    String json7 = contents("json/resource_7.json");
    Assertions.assertFalse(CatalogUtil.equals("id", json1, json7));
  }

  @Test
  void testEquals_withArrayMissing_returnsFalse() {
    String json1 = contents("json/payload1.json");
    String json2 = contents("json/payload2.json");
    Map<String, Object> map1 = CatalogUtil.readAsMap(json1);
    Map<String, Object> map2 = CatalogUtil.readAsMap(json2);
    String json1a = stripForPatch(map1);
    String json2a = stripForPatch(map2);
    Assertions.assertFalse(CatalogUtil.equals("id", json1a, json2a));
  }

  @Test
  void testEquals_withInitialAndUpdatedSpecification_testsEquals() {
    String json1 = contents("json/spec1.json");
    String json2 = contents("json/spec2.json");
    Map<String, Object> map1 = CatalogUtil.readAsMap(json1);
    Map<String, Object> map2 = CatalogUtil.readAsMap(json2);
    String json1a = stripForPatch(map1);
    String json2a = stripForPatch(map2);
    Assertions.assertTrue(CatalogUtil.equals("id", json1a, json2a));
    Assertions.assertTrue(CatalogUtil.equals("id", json1a, contents("json/spec1_stripped.json")));
    Assertions.assertTrue(CatalogUtil.equals("id", json2a, contents("json/spec2_stripped.json")));
  }
}
