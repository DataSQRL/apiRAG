package com.datasqrl.ai.models;

import com.datasqrl.ai.tool.FunctionDefinition;

public interface ModelAnalyzer<ChatMessage> {
  int countTokens(FunctionDefinition function);

  int countTokens(ChatMessage message);

  int countTokens(String generation);
}
