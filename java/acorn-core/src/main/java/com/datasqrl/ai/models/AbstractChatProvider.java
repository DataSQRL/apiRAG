package com.datasqrl.ai.models;

import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.ModelObservability;
import com.datasqrl.ai.tool.ToolManager;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class AbstractChatProvider<Message, FunctionCall> implements ChatProvider {

  public static int DEFAULT_HISTORY_LIMIT = 50;
  public static final int FUNCTION_CALL_RETRIES_LIMIT = 5;

  protected final ToolManager backend;
  @Getter
  protected final ModelBindings<Message, FunctionCall> bindings;
  @Getter
  protected final ModelObservability observability;


  @Override
  public List<GenericChatMessage> getHistory(Context sessionContext, boolean includeFunctionCalls) {
    return backend.getChatMessages(sessionContext, DEFAULT_HISTORY_LIMIT, GenericChatMessage.class).stream()
        .map(bindings::convertMessage)
        .filter(message -> includeFunctionCalls || bindings.isUserOrAssistantMessage(message))
        .map(m -> bindings.convertMessage(m, sessionContext)).toList();
  }

}
