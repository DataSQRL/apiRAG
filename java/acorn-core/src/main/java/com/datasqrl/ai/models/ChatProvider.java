package com.datasqrl.ai.models;

import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.GenericChatMessage;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class ChatProvider<Message, FunctionCall> {

  public static int DEFAULT_HISTORY_LIMIT = 50;
  public static final int FUNCTION_CALL_RETRIES_LIMIT = 5;

  protected final ToolsBackend backend;
  @Getter
  protected final ModelBindings<Message, FunctionCall> bindings;

  public abstract GenericChatMessage chat(String message, Map<String, Object> context);

  public List<GenericChatMessage> getHistory(Map<String, Object> sessionContext, boolean includeFunctionCalls) {
    return backend.getChatMessages(sessionContext, DEFAULT_HISTORY_LIMIT, GenericChatMessage.class).stream()
        .map(bindings::convertMessage)
        .filter(message -> includeFunctionCalls || bindings.isUserOrAssistantMessage(message))
        .map(m -> bindings.convertMessage(m, sessionContext)).toList();
  }

}
