package com.datasqrl.ai.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.datasqrl.ai.util.ConfigurationUtil;
import java.net.URL;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class DataAgentConfigurationTest {

  public static final Path GRAPHQL_RESOURCES = Path.of("..", "apirag-graphql", "src", "test", "resources", "graphql");
  public static final Path CORE_RESOURCES = Path.of("..", "apirag-core", "src" , "test", "resources");
  public static final Path CONFIG_RESOURCES = Path.of("src", "test", "resources", "config");

  @Test
  public void testNutshopConfiguration() {
    Path nutshopGraphQl = GRAPHQL_RESOURCES.resolve("nutshop-schema.graphqls");
    Path nutshopTools = CORE_RESOURCES.resolve("nutshop-c360.tools.json");
    Path nutshopConfig = CONFIG_RESOURCES.resolve("nutshop.config.json");

    DataAgentConfiguration config1 = DataAgentConfiguration.fromFile(nutshopConfig, nutshopTools);
    DataAgentConfiguration config2 = DataAgentConfiguration.fromFile(nutshopConfig, nutshopGraphQl);

    assertEquals(4, config1.getFunctionBackend().getFunctions().size());
    assertEquals(5, config2.getFunctionBackend().getFunctions().size());
  }

}
