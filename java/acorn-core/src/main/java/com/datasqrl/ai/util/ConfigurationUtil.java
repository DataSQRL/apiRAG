package com.datasqrl.ai.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Resources;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;

@Slf4j
public class ConfigurationUtil {

  public static String getEnvOrSystemVariable(String name) {
    String env = null;
    try {
      env = System.getenv(name);
    } catch (SecurityException e) {
      log.warn("Could not read environmental variables", e);
    }
    if (env==null) {
      env = System.getProperty(name);
    }
    ErrorHandling.checkArgument(env!=null && !env.isBlank(), "Need to configure environmental variable `%s`", name);
    return env;
  }

  public static <T extends Enum<T>> Optional<T> getEnumFromString(Class<T> enumClazz, String name) {
    if (enumClazz != null && name != null && !name.isEmpty()) {
      for (T val :enumClazz.getEnumConstants()) {
        if (val.name().equalsIgnoreCase(name)) return Optional.of(val);
      }
    }
    return Optional.empty();
  }

  public static Set<String> getSubKeys(Configuration config) {
    Set<String> keys = new HashSet<>();
    Iterator<String> keyIter = config.getKeys();
    while (keyIter.hasNext()) {
      String key =  keyIter.next();
      keys.add(key.split("\\.")[0]);
    }
    return keys;
  }

  public static URL getResourceFile(String path) {
    URL url = ConfigurationUtil.class.getClassLoader().getResource(path);
    ErrorHandling.checkArgument(url!=null, "Invalid url: %s", url);
    return url;
  }

  @SneakyThrows
  public static String getResourcesFileAsString(String path) {
    return Resources.toString(getResourceFile(path), StandardCharsets.UTF_8);
  }

  public static String getFileExtension(Path filePath) {
    String fileName = filePath.getFileName().toString();
    int lastIndexOfDot = fileName.lastIndexOf(".");
    if (lastIndexOfDot == -1) {
      return ""; // No extension found
    }
    return fileName.substring(lastIndexOfDot + 1).trim().toLowerCase();
  }

  public static Configuration jsonToConfiguration(JsonNode jsonNode) {
    if (jsonNode.isEmpty()) return new BaseConfiguration();
    Map<String, Object> map = new HashMap<>();
    Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      map.put(field.getKey(), field.getValue().asText());
    }
    return new MapConfiguration(map);
  }

}
