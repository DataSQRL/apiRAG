package com.datasqrl.ai.spring;

import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.GenericFunctionCall;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponseMessage {

  private String role;
  private String content;
  private FunctionResponse clientFunction;
  private String uuid;
  private String timestamp;

  public static ResponseMessage from(GenericChatMessage message) {
    return new ResponseMessage(message.getRole(), message.getFunctionCall() == null ? message.getContent() : null,
        FunctionResponse.from(message.getFunctionCall()), message.getUuid(), message.getTimestamp());
  }

  public static ResponseMessage system(String content) {
    return new ResponseMessage("system", content, null, UUID.randomUUID().toString(),
        Instant.now().toString());
  }

  @Data
  @AllArgsConstructor
  public static class FunctionResponse {

    String name;
    JsonNode arguments;
    
    public static FunctionResponse from(GenericFunctionCall fct) {
      return fct==null?null:new FunctionResponse(fct.getName(), fct.getArguments());
    }

  }

}
