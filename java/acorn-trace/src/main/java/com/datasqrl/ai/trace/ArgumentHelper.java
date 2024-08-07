package com.datasqrl.ai.trace;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ArgumentHelper {

  public static <T> List<T> asList(JsonNode jsonNode, Function<JsonNode, T> getValue) {
    if (jsonNode.isEmpty()) return List.of();
    if (!jsonNode.isArray()) throw new IllegalArgumentException("Not an array");
    List<T> result = new ArrayList<>();
    jsonNode.forEach(n -> result.add(getValue.apply(n)));
    return result;
  }

  public static List<Double> asDoubleList(JsonNode jsonNode) {
    return asList(jsonNode, JsonNode::asDouble);
  }

  public static List<String> asStringList(JsonNode jsonNode) {
    return asList(jsonNode, JsonNode::asText);
  }


}
