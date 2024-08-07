package com.datasqrl.ai.models.groq;

import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.GenericFunctionCall;
import com.datasqrl.ai.models.ModelAnalyzer;
import com.datasqrl.ai.models.ModelBindings;
import com.fasterxml.jackson.databind.JsonNode;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.completion.chat.FunctionMessage;
import com.theokanning.openai.completion.chat.SystemMessage;
import com.theokanning.openai.completion.chat.ToolMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import java.time.Instant;
import java.util.Map;

public class GroqModelBindings implements ModelBindings<ChatMessage, ChatFunctionCall> {

  GroqModelConfiguration model;
  GroqTokenCounter tokenCounter;

  public GroqModelBindings(GroqModelConfiguration model) {
    this.model = model;
    this.tokenCounter = GroqTokenCounter.of(model);
  }


  @Override
  public ChatMessage convertMessage(GenericChatMessage message) {
    ChatMessageRole role = ChatMessageRole.valueOf(message.getRole().toUpperCase());
    //Parse function call?
    return switch (role) {
      case SYSTEM -> new SystemMessage(message.getContent(), message.getName());
      case USER -> new UserMessage(message.getContent(), message.getName());
      case ASSISTANT -> new AssistantMessage(message.getContent(), message.getName());
      case FUNCTION -> new FunctionMessage(message.getContent(), message.getName());
      case TOOL -> new ToolMessage(message.getContent(), message.getName());
    };
  }


  @Override
  public GenericChatMessage convertMessage(ChatMessage msg, Context sessionContext) {
    ChatFunctionCall fctCall = null;
    if (ChatMessageRole.valueOf(msg.getRole().toUpperCase()) == ChatMessageRole.ASSISTANT) {
      fctCall = ((AssistantMessage) msg).getFunctionCall();
    }
    return GenericChatMessage.builder()
        .role(msg.getRole())
        .content(fctCall == null ? msg.getTextContent() : functionCall2String(fctCall))
        .functionCall(fctCall == null ? null :  new GenericFunctionCall(fctCall.getName(),fctCall.getArguments()))
        .name(msg.getName())
        .context(sessionContext.asMap())
        .timestamp(Instant.now().toString())
        .numTokens(tokenCounter.countTokens(msg))
        .build();
  }

  @Override
  public boolean isUserOrAssistantMessage(ChatMessage chatMessage) {
    return ChatMessageRole.valueOf(chatMessage.getRole().toUpperCase()) == ChatMessageRole.ASSISTANT
        || ChatMessageRole.valueOf(chatMessage.getRole().toUpperCase()) == ChatMessageRole.USER;
  }

  @Override
  public ModelAnalyzer<ChatMessage> getTokenCounter() {
    return tokenCounter;
  }

  @Override
  public int getMaxInputTokens() {
    return model.getMaxInputTokens();
  }

  @Override
  public ChatMessage createSystemMessage(String systemMessage) {
    return new SystemMessage(systemMessage);
  }

  @Override
  public String getFunctionName(ChatFunctionCall functionCall) {
    return functionCall.getName();
  }

  @Override
  public JsonNode getFunctionArguments(ChatFunctionCall functionCall) {
    return functionCall.getArguments();
  }

  @Override
  public FunctionMessage newFunctionResultMessage(String functionName, String functionResult) {
    return new FunctionMessage(functionResult, functionName);
  }

  @Override
  public ChatMessage convertExceptionToMessage(String error) {
    return new UserMessage("{\"error\": \"" + error + "\"}", "error");
  }

  @Override
  public String getTextContent(ChatMessage chatMessage) {
    return chatMessage.getTextContent();
  }

  @Override
  public ChatMessage newUserMessage(String text) {
    return new UserMessage(text);
  }

  private static String functionCall2String(ChatFunctionCall fctCall) {
    return "{"
        + "\"function\": \"" + fctCall.getName() + "\", "
        + "\"parameters\": " + fctCall.getArguments().toString()
        + "}";
  }

}
