package org.opentmf.catalog.sync.util;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.opentmf.catalog.sync.model.CatalogConstants;
import org.opentmf.catalog.sync.model.CatalogType;
import org.opentmf.catalog.sync.model.SingleContext;
import org.opentmf.commons.util.JacksonUtil;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Gokhan Demir
 */
@Slf4j
public final class CatalogUtil {

  @Generated
  private CatalogUtil() {
  }

  private static final Resource[] NO_RESOURCES = new Resource[]{};

  private static final List<String> STRIP_POST = Arrays.asList(
      CatalogConstants.HREF,
      CatalogConstants.REVISION,
      CatalogConstants.VALID_FOR,
      CatalogConstants.ACL_RELATED_PARTY,
      CatalogConstants.LAST_UPDATE,
      CatalogConstants.CREATED_DATE,
      CatalogConstants.UPDATED_DATE,
      CatalogConstants.CREATED_BY,
      CatalogConstants.UPDATED_BY,
      CatalogConstants.AT_SCHEMA_LOCATION
  );

  private static final List<String> STRIP_PATCH = Arrays.asList(
      CatalogConstants.ID,
      CatalogConstants.VERSION,
      CatalogConstants.AT_BASE_TYPE,
      CatalogConstants.AT_TYPE
  );

  public static Map<String, Object> readAsMap(Resource json) {
    try {
      return JacksonUtil.jsonToMap(JacksonUtil.contents(json.getInputStream()));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static Map<String, Object> readAsMap(String json) {
    return JacksonUtil.jsonToMap(json);
  }

  public static String stripForPost(Map<String, Object> map) {
    Map<String, Object> copy = new LinkedHashMap<>(map);
    STRIP_POST.forEach(copy::remove);
    return JacksonUtil.objectToJson(copy);
  }

  public static String stripForPatch(Map<String, Object> map) {
    Map<String, Object> copy = new LinkedHashMap<>(map);
    STRIP_POST.forEach(copy::remove);
    STRIP_PATCH.forEach(copy::remove);
    return JacksonUtil.objectToJson(copy);
  }

  public static String version0(String json) {
    var tree = (ObjectNode) JacksonUtil.jsonToTree(json);
    tree.put(CatalogConstants.VERSION, "0");
    tree.put("lifecycleStatus", "In design");
    var validFor = JacksonUtil.getDefaultJsonMapper().createObjectNode();
    validFor.put("startDateTime",
        ZonedDateTime.now(ZoneId.of("UTC")).minusYears(10).format(ISO_OFFSET_DATE_TIME));
    validFor.put("endDateTime",
        ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_OFFSET_DATE_TIME));
    tree.replace(CatalogConstants.VALID_FOR, validFor);
    return JacksonUtil.objectToJson(tree);
  }

  public static String launchedVersion(String json) {
    var tree = (ObjectNode) JacksonUtil.jsonToTree(json);
    tree.put(CatalogConstants.VERSION, "1");
    tree.put("lifecycleStatus", "Launched");
    var validFor = JacksonUtil.getDefaultJsonMapper().createObjectNode();
    validFor.put("startDateTime",
        ZonedDateTime.now(ZoneId.of("UTC")).plusMinutes(1L).format(ISO_OFFSET_DATE_TIME));
    tree.replace(CatalogConstants.VALID_FOR, validFor);
    return JacksonUtil.objectToJson(tree);
  }

  /**
   * Returns true if the stripped versions of the requested and existing payloads are equal
   * in {@link JSONCompareMode#NON_EXTENSIBLE}.
   */
  public static boolean requestedEqualsExisting(SingleContext ctx) {
    String json1 = stripForPatch(ctx.getRequestedCatalog());
    String json2 = stripForPatch(ctx.getExistingCatalog());
    return equals(ctx.getId(), json1, json2);
  }

  public static boolean equals(String id, String json1, String json2) {
    try {
      var compareResult = JSONCompare.compareJSON(json1, json2, JSONCompareMode.NON_EXTENSIBLE);
      if (compareResult.failed() && log.isTraceEnabled()) {
        log.trace("{}: Payloads not equal in NON_EXTENSIBLE mode: {}\nPayload1: {}\nPayload2: {}",
            id, compareResult, json1, json2);
      }
      return compareResult.passed();
    } catch (JSONException e) {
      log.warn("{}: JSON comparison failed, treating as not equal.", id, e);
      return false;
    }
  }

  public static Resource[] getCatalogs(CatalogType catalogType) {
    try {
      return new PathMatchingResourcePatternResolver()
          .getResources("classpath:catalog/" + catalogType.getLocationPattern() + "/**/*.json");
    } catch (IOException e) {
      log.debug("No catalog files were found at {}", catalogType.getLocationPattern());
      return NO_RESOURCES;
    }
  }

  public static Resource[] getAllCatalogs() {
    try {
      return new PathMatchingResourcePatternResolver()
          .getResources("classpath:catalog/**/*.json");
    } catch (IOException e) {
      return NO_RESOURCES;
    }
  }

  public static Resource[] getCatalogs(String locationPattern) {
    try {
      return new PathMatchingResourcePatternResolver()
          .getResources("classpath:catalog/" + locationPattern + "/**/*.json");
    } catch (IOException e) {
      return NO_RESOURCES;
    }
  }
}
