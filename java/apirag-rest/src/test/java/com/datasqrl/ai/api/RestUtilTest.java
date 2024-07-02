package com.datasqrl.ai.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.datasqrl.ai.api.RestUtil.DecomposedURL;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class RestUtilTest {

  private ObjectMapper objectMapper;
  private APIQuery query;

  @BeforeEach
  public void setup() {
    objectMapper = new ObjectMapper();
    query = new APIQuery("myName", null, "/path/{param1}/resource/{param2}?param3={param3}&param4={param4}&staticFilter&anotherFilter=5", "POST");
  }

  @Test
  public void testExtractPathParameters() {
    Set<String> parameters = RestUtil.extractPathParameters("/path/{param1}/my-resource/{my-param2}");
    assertEquals(2, parameters.size());
    assertTrue(parameters.contains("param1"));
    assertTrue(parameters.contains("my-param2"));
  }

  @Test
  public void testDecomposedURL() {
    DecomposedURL decomposedURL = RestUtil.decomposedURL(query.getPath());

    assertEquals("/path/{param1}/resource/{param2}?staticFilter&anotherFilter=5", decomposedURL.path());
    assertEquals(2, decomposedURL.pathParams().size());
    assertTrue(decomposedURL.pathParams().contains("param1"));
    assertTrue(decomposedURL.pathParams().contains("param2"));
    assertEquals(2, decomposedURL.queryParams().size());
    assertTrue(decomposedURL.queryParams().containsKey("param3"));
    assertTrue(decomposedURL.queryParams().containsKey("param4"));
    assertEquals(2, decomposedURL.numStaticFilters());
  }

  @Test
  public void testCreateRestCall() {
    JsonNode arguments = objectMapper.createObjectNode().put("param1", "value1").put("param2", "value2").put("extra", "extraValue");
    RestUtil.RestCall restCall = RestUtil.createRestCall(query, arguments);

    assertEquals("/path/value1/resource/value2?staticFilter&anotherFilter=5", restCall.path());
    assertEquals("POST", restCall.method());

    JsonNode body = restCall.body();
    assertTrue(body.has("extra"));
    assertEquals("extraValue", body.get("extra").asText());
    assertFalse(body.has("param1"));
    assertFalse(body.has("param2"));
  }

  @Test
  public void testCreateRestCallNoBody() {
    JsonNode arguments = objectMapper.createObjectNode().put("param1", "value1").put("param2", "value2").put("param4", "4");
    RestUtil.RestCall restCall = RestUtil.createRestCall(query, arguments);

    assertEquals("/path/value1/resource/value2?staticFilter&anotherFilter=5&param4=4", restCall.path());
    assertEquals("POST", restCall.method());

    JsonNode body = restCall.body();
    assertTrue(body.isEmpty());
  }

  @Test
  public void testRemoveFields() {
    ObjectNode json = objectMapper.createObjectNode().put("field1", "value1").put("field2", "value2");
    Set<String> fieldsToRemove = Set.of("field1");
    JsonNode result = RestUtil.removeFields(json, fieldsToRemove);

    assertEquals(1, result.size());
    assertFalse(result.has("field1"));
    assertTrue(result.has("field2"));
  }
}
