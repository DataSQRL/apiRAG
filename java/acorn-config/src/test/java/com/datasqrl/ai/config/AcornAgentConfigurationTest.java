package com.datasqrl.ai.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class AcornAgentConfigurationTest {

  public static final Path GRAPHQL_RESOURCES = Path.of("..", "acorn-graphql", "src", "test", "resources", "graphql");
  public static final Path CORE_RESOURCES = Path.of("..", "acorn-core", "src" , "test", "resources");
  public static final Path CONFIG_RESOURCES = Path.of("src", "test", "resources", "config");

  @Test
  public void testNutshopConfiguration() throws IOException {
    Path nutshopGraphQl = GRAPHQL_RESOURCES.resolve("nutshop-schema.graphqls");
    Path nutshopTools = CORE_RESOURCES.resolve("nutshop-c360.tools.json");
    Path nutshopConfig = CONFIG_RESOURCES.resolve("nutshop.config.json");

    AcornAgentConfiguration config1 = AcornAgentConfiguration.fromFile(nutshopConfig, nutshopTools);
    AcornAgentConfiguration config2 = AcornAgentConfiguration.fromFile(nutshopConfig, nutshopGraphQl);

    assertEquals(5, config1.getToolManager().getFunctions().size());
    assertEquals(7, config2.getToolManager().getFunctions().size());
  }

}
