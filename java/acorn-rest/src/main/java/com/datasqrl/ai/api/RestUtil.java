package com.datasqrl.ai.api;

import com.datasqrl.ai.util.ErrorHandling;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.SneakyThrows;


public class RestUtil {

  public record RestCall(String path, String method, JsonNode body) {}

  /**
   * Creates a RestCall object based on the APIQuery and JsonNode arguments provided.
   *
   * @param  query     the APIQuery object containing query information
   * @param  arguments the JsonNode object containing arguments for the query
   * @return           the RestCall object representing the REST call
   */
  @SneakyThrows
  public static RestCall createRestCall(APIQuery query, JsonNode arguments) {
    DecomposedURL decomposedURL = decomposedURL(query.getPath());
    String expandedPath = decomposedURL.path();
    for (String pathParam : decomposedURL.pathParams()) {
      ErrorHandling.checkArgument(arguments.has(pathParam), "Missing argument: %s", pathParam);
      String replaceValue = arguments.get(pathParam).asText();
      replaceValue = URLEncoder.encode(replaceValue, StandardCharsets.UTF_8);
      String placeholder = String.format("{%s}", pathParam);
      expandedPath = expandedPath.replace(placeholder, replaceValue);
    }
    int numFilters = decomposedURL.numStaticFilters();
    for (Map.Entry<String,String> filter : decomposedURL.queryParams().entrySet()) {
      if (arguments.has(filter.getValue())) {
        String value = arguments.get(filter.getValue()).asText();
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
        if (numFilters>0) {
          expandedPath += "&";
        }
        expandedPath += filter.getKey() + "=" + encodedValue;
        numFilters++;
      }
    }
    Set<String> allArguments = new HashSet<>();
    allArguments.addAll(decomposedURL.pathParams());
    allArguments.addAll(decomposedURL.queryParams().values());
    JsonNode body = removeFields(arguments, allArguments);
    return new RestCall(expandedPath, query.getMethod().trim().toUpperCase(), body);
  }

  /**
   * Creates a deep copy of the json object but removes the specified fields
   *
   * @param json
   * @param fields
   * @return
   */
  static JsonNode removeFields(JsonNode json, Set<String> fields) {
    if (fields.isEmpty()) return json;
    ObjectNode copy = json.deepCopy();
    fields.forEach(copy::remove);
    return copy;
  }

  private static final Pattern ARGUMENT_PATTERN = Pattern.compile("\\{(.*?)}");

  public static Set<String> extractPathParameters(String path) {
    Matcher matcher = ARGUMENT_PATTERN.matcher(path);
    Set<String> parameters = new HashSet<>();
    while (matcher.find()) {
      parameters.add(matcher.group(1));
    }
    return parameters;
  }

  record DecomposedURL(String path, Set<String> pathParams, Map<String,String> queryParams, int numStaticFilters) {}

  public static DecomposedURL decomposedURL(String url) {
    String[] urlParts = url.split("\\?");
    String path = urlParts[0];
    int numStaticFilters = 0;
    Map<String,String> queryParams = new HashMap<>();
    if (urlParts.length > 1) {
      path = path + "?";
      String query = urlParts[1];
      String[] pairs = query.split("&");
      for (String pair : pairs) {
        String[] keyValue = pair.split("=");
        if (keyValue.length > 1) {
          Matcher matcher = ARGUMENT_PATTERN.matcher(keyValue[1]);
          if (matcher.matches()) {
            queryParams.put(keyValue[0], matcher.group(1));
            continue;
          }
        }
        //If we didn't continue, add as static filter
        if (numStaticFilters>0) {
          path = path + "&";
        }
        path = path + pair;
        numStaticFilters++;
      }
    }
    return new DecomposedURL(path, extractPathParameters(urlParts[0]), queryParams, numStaticFilters);
  }

}
