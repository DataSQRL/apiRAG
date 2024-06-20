package com.datasqrl.ai.backend;

public interface ModelAnalyzer<ChatMessage> {
  int countTokens(FunctionDefinition function);

  int countTokens(ChatMessage message);

  int countTokens(String generation);
}
