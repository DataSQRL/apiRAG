package com.datasqrl.ai.models.bedrock;

import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.GenericFunctionCall;
import com.datasqrl.ai.models.ModelAnalyzer;
import com.datasqrl.ai.models.ModelBindings;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Map;

public class BedrockModelBindings implements ModelBindings<BedrockChatMessage, BedrockFunctionCall> {

  private final BedrockModelConfiguration model;
  BedrockTokenCounter tokenCounter;

  public BedrockModelBindings(BedrockModelConfiguration model) {
    this.model = model;
    this.tokenCounter = BedrockTokenCounter.of(model);
  }

  @Override
  public BedrockChatMessage convertMessage(GenericChatMessage message) {
    BedrockChatRole role = BedrockChatRole.valueOf(message.getRole().toUpperCase());
    //Parse function call?
    return switch (role) {
      case SYSTEM -> new BedrockChatMessage(BedrockChatRole.SYSTEM, message.getContent(), message.getName());
      case USER -> new BedrockChatMessage(BedrockChatRole.USER, message.getContent(), message.getName());
      case ASSISTANT -> new BedrockChatMessage(BedrockChatRole.ASSISTANT, message.getContent(), message.getName());
      case FUNCTION -> new BedrockChatMessage(BedrockChatRole.FUNCTION, message.getContent(), message.getName());
    };
  }

  @Override
  public GenericChatMessage convertMessage(BedrockChatMessage msg, Context sessionContext) {
    BedrockFunctionCall fctCall = null;
    if (msg.getRole() == BedrockChatRole.ASSISTANT) {
      fctCall = msg.getFunctionCall();
    }
    return GenericChatMessage.builder()
        .role(msg.getRole().getRole())
        .content(fctCall == null ? msg.getTextContent() : functionCall2String(fctCall))
        .functionCall(fctCall == null ? null :  new GenericFunctionCall(fctCall.getFunctionName(),fctCall.getArguments()))
        .name(msg.getName())
        .context(sessionContext.asMap())
        .timestamp(Instant.now().toString())
        .numTokens(tokenCounter.countTokens(msg))
        .build();
  }

  @Override
  public BedrockChatMessage convertExceptionToMessage(String error) {
    return new BedrockChatMessage(BedrockChatRole.USER, "{\"error\": \"" + error + "\"}", "error");
  }

  @Override
  public String getTextContent(BedrockChatMessage message) {
    return message.getTextContent();
  }

  @Override
  public BedrockChatMessage newUserMessage(String text) {
    return new BedrockChatMessage(BedrockChatRole.USER, text, "");
  }

  @Override
  public boolean isUserOrAssistantMessage(BedrockChatMessage chatMessage) {
    return chatMessage.getRole() == BedrockChatRole.USER
        || chatMessage.getRole() == BedrockChatRole.ASSISTANT;
  }

  @Override
  public ModelAnalyzer<BedrockChatMessage> getTokenCounter() {
    return tokenCounter;
  }

  @Override
  public int getMaxInputTokens() {
    return model.getMaxInputTokens();
  }

  @Override
  public BedrockChatMessage createSystemMessage(String systemMessage) {
    return new BedrockChatMessage(BedrockChatRole.SYSTEM, systemMessage, "");
  }

  @Override
  public String getFunctionName(BedrockFunctionCall functionCall) {
    return functionCall.getFunctionName();
  }

  @Override
  public JsonNode getFunctionArguments(BedrockFunctionCall functionCall) {
    return functionCall.getArguments();
  }

  @Override
  public BedrockChatMessage newFunctionResultMessage(String functionName, String functionResult) {
    return new BedrockChatMessage(BedrockChatRole.FUNCTION, functionResult, functionName);
  }

  private static String functionCall2String(BedrockFunctionCall fctCall) {
    return "{"
        + "\"function\": \"" + fctCall.getFunctionName() + "\", "
        + "\"parameters\": " + fctCall.getArguments().toString()
        + "}";
  }

}
