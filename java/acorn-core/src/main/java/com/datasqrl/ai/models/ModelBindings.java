package com.datasqrl.ai.models;

import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public interface ModelBindings<Message, FunctionCall> {

  Message convertMessage(GenericChatMessage message);

  GenericChatMessage convertMessage(Message message, Context sessionContext);

  boolean isUserOrAssistantMessage(Message message);

  ModelAnalyzer<Message> getTokenCounter();

  int getMaxInputTokens();

  Message createSystemMessage(String systemMessage);

  String getFunctionName(FunctionCall functionCall);

  JsonNode getFunctionArguments(FunctionCall functionCall);

  Message newFunctionResultMessage(String functionName, String functionResult);

  Message convertExceptionToMessage(String s);

  String getTextContent(Message message);

  Message newUserMessage(String text);
}
