package com.pia.catalog.sync.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pia.commons.util.JacksonUtil;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.core.io.Resource;

/**
 * @author Gokhan Demir
 */
@Slf4j
public final class JacksonUtil2 {

  private static final ObjectMapper OBJECT_MAPPER = JacksonUtil.getDefaultObjectMapper();

  private JacksonUtil2() {
  }

  public static SortedMap<String, Object> readAsMap(Resource json) {
    try {
      return OBJECT_MAPPER.readValue(json.getInputStream(),
          new TypeReference<TreeMap<String, Object>>() {});
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static SortedMap<String, Object> readAsMap(String json) {
    try {
      return OBJECT_MAPPER.readValue(json, new TypeReference<TreeMap<String, Object>>() {});
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Returns true if the stripped versions of the requested and existing payloads are equal in
   * JSONCompareMode.NON_EXTENSIBLE
   *
   * @param id The id of the catalog object
   * @param json1 Requested stripped payload
   * @param json2 Existing stripped payload
   * @return true if the stripped versions of the requested and existing payloads are equal in
   *     JSONCompareMode.NON_EXTENSIBLE
   * @see JSONCompareMode#NON_EXTENSIBLE
   */
  public static boolean equals(String id, String json1, String json2) {
    try {
      var compareResult = JSONCompare.compareJSON(json1, json2, JSONCompareMode.NON_EXTENSIBLE);
      if (compareResult.failed() && log.isTraceEnabled()) {
        log.trace("{}: Payloads not equal in NON_EXTENSIBLE mode: {}\nPayload1: {}\nPayload2: {}",
            id, compareResult, json1, json2);
      }
      return compareResult.passed();
    } catch (Exception ignored) {
      return false;
    }
  }
}
