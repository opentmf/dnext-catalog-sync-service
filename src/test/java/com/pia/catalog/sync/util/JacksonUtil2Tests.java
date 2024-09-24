package com.pia.catalog.sync.util;

import static com.pia.catalog.sync.util.CatalogUtil.stripForPatch;
import static com.pia.commons.util.JacksonUtil.contents;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Gokhan Demir
 */
class JacksonUtil2Tests {

  @Test
  void testEquals_withEqualObjectsButDifferentCharacteristicOrder_returnsTrue() {
    String json1 = contents("json/resource_1.json");
    //Same object different characteristic order
    String json2 = contents("json/resource_2.json");
    Assertions.assertTrue(JacksonUtil2.equals("id", json1, json2));

    //Same object different field order
    String json3 = contents("json/resource_3.json");
    Assertions.assertTrue(JacksonUtil2.equals("id", json1, json3));

    //Same object different field order inside object
    String json4 = contents("json/resource_4.json");
    Assertions.assertTrue(JacksonUtil2.equals("id", json1, json4));
  }

  @Test
  void testEquals_withNonEqualObjects_returnsFalse() {
    String json1 = contents("json/resource_1.json");
    //Different characteristic
    String json5 = contents("json/resource_5.json");
    Assertions.assertFalse(JacksonUtil2.equals("id", json1, json5));

    //Different field
    String json6 = contents("json/resource_6.json");
    Assertions.assertFalse(JacksonUtil2.equals("id", json1, json6));

    //Different field inside object
    String json7 = contents("json/resource_7.json");
    Assertions.assertFalse(JacksonUtil2.equals("id", json1, json7));
  }

  @Test
  void testEquals_withArrayMissing_returnsFalse() {
    String json1 = contents("json/payload1.json");
    String json2 = contents("json/payload2.json");
    Map<String, Object> map1 = JacksonUtil2.readAsMap(json1);
    Map<String, Object> map2 = JacksonUtil2.readAsMap(json2);
    String json1a = stripForPatch(map1);
    String json2a = stripForPatch(map2);
    Assertions.assertFalse(JacksonUtil2.equals("id", json1a, json2a));
  }

  @Test
  void testEquals_withInitialAndUpdatedSpecification_testsEquals() {
    String json1 = contents("json/spec1.json");
    String json2 = contents("json/spec2.json");
    Map<String, Object> map1 = JacksonUtil2.readAsMap(json1);
    Map<String, Object> map2 = JacksonUtil2.readAsMap(json2);
    String json1a = stripForPatch(map1);
    String json2a = stripForPatch(map2);
    Assertions.assertTrue(JacksonUtil2.equals("id", json1a, json2a));
    Assertions.assertEquals(json1a, contents("json/spec1_stripped.json"));
    Assertions.assertEquals(json2a, contents("json/spec2_stripped.json"));
  }
}
