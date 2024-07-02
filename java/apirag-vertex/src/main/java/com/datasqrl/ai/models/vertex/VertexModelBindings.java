package com.datasqrl.ai.models.vertex;

import com.datasqrl.ai.backend.GenericChatMessage;
import com.datasqrl.ai.backend.GenericFunctionCall;
import com.datasqrl.ai.backend.ModelAnalyzer;
import com.datasqrl.ai.backend.ModelBindings;
import com.datasqrl.ai.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.FunctionCall;
import com.google.cloud.vertexai.api.FunctionResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class VertexModelBindings implements ModelBindings<Content, FunctionCall> {

  private final VertexChatModel model;
  private final GenerativeModel generativeModel;
  private final VertexTokenCounter tokenCounter;


  public VertexModelBindings(VertexChatModel model, String vertexProjectId, String vertexProjectLocation) {
    this.model = model;
    VertexAI vertexAI = new VertexAI(vertexProjectId, vertexProjectLocation);
    this.generativeModel = new GenerativeModel(model.modelName, vertexAI);
    this.tokenCounter = VertexTokenCounter.of(generativeModel);
  }

  @Override
  public Content convertMessage(GenericChatMessage message) {
    return switch (message.getRole()) {
      case "model" -> {
        Content.Builder msgBuilder = Content.newBuilder().setRole("model");
        Part.Builder partBuilder = Part.newBuilder().setText(message.getContent());
        GenericFunctionCall functionCall = message.getFunctionCall();
        if (functionCall != null) {
          FunctionCall.Builder fctCallBuilder = FunctionCall.newBuilder()
              .setName(functionCall.getName())
              .setArgs(ProtobufUtils.jsonNodeToStruct(functionCall.getArguments()));
          partBuilder.setFunctionCall(fctCallBuilder);
        }
        yield msgBuilder.addParts(partBuilder).build();
      }
//      TODO: Revisit! System and function are not Gemini roles. Need to encode them differently internally
      case "system" -> ContentMaker.forRole("system").fromString(message.getContent());
      case "function" -> {
        Content.Builder msgBuilder = Content.newBuilder().setRole("function");
        String functionName = message.getName();
        Optional<JsonNode> responseJson = JsonUtil.parseJson(message.getContent());
        if (responseJson.isPresent()) {
          FunctionResponse.Builder functionResponseBuilder = FunctionResponse.newBuilder()
              .setName(functionName)
              .setResponse(ProtobufUtils.jsonNodeToStruct(responseJson.get()));
          msgBuilder.addParts(Part.newBuilder().setFunctionResponse(functionResponseBuilder));
        } else {
          msgBuilder.addParts(Part.newBuilder().setText(message.getContent()));
        }
        yield msgBuilder.build();
      }
      default -> ContentMaker.forRole("user").fromString(message.getContent());
    };
  }

  //  TODO: Double check
  @Override
  public GenericChatMessage convertMessage(Content content, Map<String, Object> sessionContext) {
    GenericChatMessage.GenericChatMessageBuilder builder = GenericChatMessage.builder()
        .role(content.getRole())
        .context(sessionContext)
        .timestamp(Instant.now().toString())
        .numTokens(tokenCounter.countTokens(content));
    switch (content.getRole()) {
      default -> builder.content(ProtobufUtils.contentToString(content)).name("");
      case "model" -> {
        Optional<FunctionCall> functionCall = content.getPartsList().stream().filter(Part::hasFunctionCall).map(Part::getFunctionCall).findFirst();
        if (functionCall.isPresent()) {
          FunctionCall call = functionCall.get();
          builder.functionCall(new GenericFunctionCall(call.getName(), ProtobufUtils.structToJsonNode(call.getArgs())))
              .name(call.getName())
              .content(functionCall2String(call));
        } else {
          builder.name("").content(ProtobufUtils.contentToString(content));
        }
      }
      case "function" -> {
        Optional<FunctionResponse> functionResponse = content.getPartsList().stream().filter(Part::hasFunctionResponse).map(Part::getFunctionResponse).findFirst();
        builder.content(functionResponse.map(response -> ProtobufUtils.structToJsonNode(response.getResponse()).toString()).orElseGet(() -> ProtobufUtils.contentToString(content)))
            .name(functionResponse.map(FunctionResponse::getName).orElse(""));
      }
    }
    return builder.build();
  }

  @Override
  public boolean isUserOrAssistantMessage(Content content) {
    return content.getRole().equals("model") || content.getRole().equals("user");
  }

  @Override
  public ModelAnalyzer<Content> getTokenCounter() {
    return tokenCounter;
  }

  //  TODO: This method is the same in all bindings
  @Override
  public int getMaxInputTokens() {
    return model.getContextWindowLength() - model.getCompletionLength();
  }

  @Override
  public Content createSystemMessage(String systemMessage) {
    return ContentMaker.forRole("system").fromString(systemMessage);
  }

  @Override
  public String getFunctionName(FunctionCall functionCall) {
    return functionCall.getName();
  }

  @Override
  public JsonNode getFunctionArguments(FunctionCall functionCall) {
    return ProtobufUtils.structToJsonNode(functionCall.getArgs());
  }

  @Override
  public Content newFunctionResultMessage(String functionName, String functionResult) {
    Optional<JsonNode> jsonNode = JsonUtil.parseJson(functionResult);
    FunctionResponse.Builder responseBuilder = FunctionResponse.newBuilder().setName(functionName);
    jsonNode.map(node -> responseBuilder.setResponse(ProtobufUtils.jsonNodeToStruct(node)));
    return Content.newBuilder().setRole("function").addParts(Part.newBuilder().setFunctionResponse(responseBuilder)).build();
  }

  @Override
  public Content convertExceptionToMessage(String s) {
    return ContentMaker.fromString(s);
  }

  @Override
  public String getTextContent(Content content) {
    return ProtobufUtils.contentToString(content);
  }

  @Override
  public Content newUserMessage(String text) {
    return ContentMaker.forRole("user").fromString(text);
  }

  private String functionCall2String(FunctionCall fctCall) {
    return "{"
        + "\"function\": \"" + fctCall.getName() + "\", "
        + "\"parameters\": " + fctCall.getArgs()
        + "}";
  }

}
