package com.datasqrl.ai.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.APIExecutorFactory;
import com.datasqrl.ai.api.APIQuery;
import com.datasqrl.ai.api.MockAPIExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class FunctionBackendTest {

  @SneakyThrows
  public static List<RuntimeFunctionDefinition> getNutshopFunctions() {
    String tools = Files.readString(Path.of("src" , "test", "resources", "nutshop-c360.tools.json"));
    return FunctionBackendFactory.parseTools(tools);
  }

  @Test
  public void readNutshop() throws Exception {
    FunctionBackend fctExec = FunctionBackendFactory.of(getNutshopFunctions(),
        Map.of(APIExecutorFactory.DEFAULT_NAME,MockAPIExecutor.of("none")));
    List<RuntimeFunctionDefinition> chatFcts = new ArrayList<>(fctExec.getFunctions().values());
    assertEquals(3, chatFcts.size());
    for (RuntimeFunctionDefinition function : chatFcts) {
      FunctionDefinition fct = function.getChatFunction();
      if (fct.getName().equalsIgnoreCase("orders")) {
        assertEquals(Set.of("limit"),fct.getParameters().getProperties().keySet());
        assertTrue(fct.getParameters().getRequired().isEmpty());
      } else if (fct.getName().equalsIgnoreCase("ordered_products")) {
        assertTrue(fct.getParameters().getProperties().isEmpty());
        assertTrue(fct.getParameters().getRequired().isEmpty());
      } else if (fct.getName().equalsIgnoreCase("spending_by_week")) {
        assertEquals(Set.of("limit"),fct.getParameters().getProperties().keySet());
        assertTrue(fct.getParameters().getRequired().isEmpty());
      } else {
        fail(fct.getName());
      }
    }
  }

  @Test
  @SneakyThrows
  public void testMessageWriting() {
    FunctionBackend fctExec = FunctionBackendFactory.of(getNutshopFunctions(),
        Map.of(APIExecutorFactory.DEFAULT_NAME, new APIExecutor() {
          @Override
          public void validate(APIQuery query) throws IllegalArgumentException {

          }

          @Override
          public String executeQuery(APIQuery query, JsonNode arguments) throws IOException {
            assertEquals("{\"role\":\"user\",\"content\":\"hello\",\"name\":\"name\",\"functionCall\":null,\"uuid\":null,\"timestamp\":null,\"numTokens\":null,\"customerid\":23,\"session\":\"xyz\"}",
                arguments.toString());
            return "";
          }
        }));
    GenericChatMessage msg = GenericChatMessage.builder()
        .role("user")
        .content("hello")
        .name("name")
        .context(Map.of("customerid", 23, "session", "xyz"))
        .build();
    fctExec.saveChatMessage(msg);
  }

}
