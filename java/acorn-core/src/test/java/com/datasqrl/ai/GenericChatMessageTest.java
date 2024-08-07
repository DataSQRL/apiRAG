package com.datasqrl.ai;

import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.GenericFunctionCall;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.datasqrl.ai.tool.ToolsBackendTest.convert;
import static org.junit.jupiter.api.Assertions.*;

public class GenericChatMessageTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void serializationTest() throws Exception {

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

    for (GenericChatMessage msg : List.of(msg1, msg2)) {
      JsonNode node = objectMapper.valueToTree(msg);
      assertTrue(node.get("functionCall").isTextual());
      String jsonString = objectMapper.writeValueAsString(msg);
      assertFalse(jsonString.isEmpty());
      JsonNode deserialized = objectMapper.readTree(jsonString);
      assertEquals(msg, objectMapper.treeToValue(deserialized, GenericChatMessage.class));
    }


  }


}
