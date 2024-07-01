package com.datasqrl.ai.backend;

import com.datasqrl.ai.api.APIExecutor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;

public class FunctionBackendFactory {

  public static final String SAVE_CHAT_FUNCTION_NAME = "InternalSaveChatMessage";
  public static final String RETRIEVE_CHAT_FUNCTION_NAME = "InternalGetChatMessages";

  private static final ObjectMapper mapper = new ObjectMapper();

  public static List<RuntimeFunctionDefinition> parseTools(@NonNull String tools) {
    try {
      return mapper.readValue(tools,
          new TypeReference<>() {
          });
    } catch (IOException e) {
      throw new RuntimeException("Could not parse tools file", e);
    }
  }

  public static FunctionBackend of(@NonNull List<RuntimeFunctionDefinition> functions, @NonNull Map<String,APIExecutor> apiExecutors) {
    FunctionBackend backend = new FunctionBackend(apiExecutors, mapper);
    for (RuntimeFunctionDefinition function : functions) {
      if (function.getName().equalsIgnoreCase(SAVE_CHAT_FUNCTION_NAME)) {
        backend.setSaveChatFct(function);
      } else if (function.getName().equalsIgnoreCase(RETRIEVE_CHAT_FUNCTION_NAME)) {
        backend.setGetChatsFct(function);
      } else {
        backend.addFunction(function);
      }
    }
    return backend;
  }
}
