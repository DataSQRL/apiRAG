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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class FunctionBackendTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

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

  @Test
  public void messageHistoryTest() throws Exception {
    objectMapper.setConfig(objectMapper.getSerializationConfig().with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true));

    Map<String, Object> context = Map.of("customerid", 10);
    GenericChatMessage msg1 = GenericChatMessage.builder()
        .role("user")
        .content("test")
        .name("myName")
        .functionCall(new GenericFunctionCall("myFunction", convert(Map.of("arg1", 5, "arg2", "myValue"))))
        .context(context)
        .uuid(UUID.randomUUID().toString())
        .timestamp(Instant.now().toString())
        .numTokens(50)
        .build();
    GenericChatMessage msg2 = GenericChatMessage.builder()
        .role("assistant")
        .content("test2")
        .name("myName")
        .functionCall(new GenericFunctionCall("myFunction", convert(Map.of("arg1", 5, "arg2", "myValue"))))
        .context(context)
        .uuid(UUID.randomUUID().toString())
        .timestamp(Instant.now().toString())
        .numTokens(230)
        .build();


    String serializedMsg = objectMapper.writeValueAsString(msg1);

    String backendMsg = objectMapper.writeValueAsString(backendSerialize(msg1, msg2));

    FunctionBackend fctExec = FunctionBackendFactory.of(getNutshopFunctions(), Map.of(APIExecutorFactory.DEFAULT_NAME, new APIExecutor() {
      @Override
      public void validate(APIQuery query) throws IllegalArgumentException {

      }

      @Override
      public String executeQuery(APIQuery query, JsonNode arguments) throws IOException {
        assertTrue(query.getQuery().startsWith("query GetChatMessages"));
        return backendMsg;
      }

      @SneakyThrows
      @Override
      public CompletableFuture<String> executeQueryAsync(APIQuery query, JsonNode arguments) {
        assertTrue(query.getQuery().startsWith("mutation AddChatMsg"));
        assertEquals(serializedMsg, objectMapper.writeValueAsString(arguments));
        return CompletableFuture.completedFuture("mock write");
      }

    }));

    assertEquals("mock write",fctExec.saveChatMessage(msg1).get());
    List<GenericChatMessage> messages = fctExec.getChatMessages(context, 10, GenericChatMessage.class);
    assertEquals(2, messages.size());
    assertEquals(msg1, messages.get(1));
    assertEquals(msg2, messages.get(0));
  }


  @SneakyThrows
  private static JsonNode backendSerialize(GenericChatMessage... messages) {
    ArrayNode arrayNode = objectMapper.createArrayNode();
    for (GenericChatMessage msg: messages) {
      JsonNode payload = objectMapper.valueToTree(msg);
      arrayNode.add(payload);
    }
    ObjectNode msgNode = objectMapper.createObjectNode();
    msgNode.set("messages", arrayNode);
    ObjectNode data = objectMapper.createObjectNode();
    data.set("data", msgNode);
    return data;
  }

  public static JsonNode convert(Map<String,Object> data) {
    return objectMapper.valueToTree(data);
  }

}
