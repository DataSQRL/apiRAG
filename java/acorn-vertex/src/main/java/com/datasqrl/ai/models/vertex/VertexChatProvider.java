package com.datasqrl.ai.models.vertex;

import com.datasqrl.ai.models.AbstractChatProvider;
import com.datasqrl.ai.models.ChatSession;
import com.datasqrl.ai.models.ContextWindow;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.ModelObservability;
import com.datasqrl.ai.tool.ModelObservability.ModelInvocation;
import com.datasqrl.ai.tool.ToolManager;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.FunctionCall;
import com.google.cloud.vertexai.api.FunctionDeclaration;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.api.Tool;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class VertexChatProvider extends AbstractChatProvider<Content, FunctionCall> {

  private final GenerativeModel chatModel;
  private final String systemPrompt;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public VertexChatProvider(VertexModelConfiguration config, ToolManager backend, String systemPrompt, ModelObservability observability) {
    super(backend, new VertexModelBindings(config), observability);
    this.systemPrompt = systemPrompt;
    VertexAI vertexAI = new VertexAI(config.getProjectId(), config.getLocation());
    GenerationConfig generationConfig =
        GenerationConfig.newBuilder()
            .setMaxOutputTokens(config.getMaxOutputTokens())
            .setTemperature(((Double) config.getTemperature()).floatValue())
            .setTopP(((Double) config.getTopP()).floatValue())
            .setTopK(config.getTopK())
            .build();
    this.chatModel = new GenerativeModel(config.getModelName(), vertexAI)
        .withSystemInstruction(ContentMaker.fromString(systemPrompt))
        .withGenerationConfig(generationConfig)
        .withTools(getTools());
  }

  private List<Tool> getTools() {
    Tool.Builder toolBuilder = Tool.newBuilder();
    this.backend.getFunctions().values().stream()
        .map(RuntimeFunctionDefinition::getChatFunction)
        .map(fromValue -> objectMapper.convertValue(fromValue, JsonNode.class))
        .map(f -> {
          capitalizeObjectTypes(f);
          try {
            String funDef = objectMapper.writeValueAsString(f);
            FunctionDeclaration.Builder builder = FunctionDeclaration.newBuilder();
            try {
              JsonFormat.parser().merge(funDef, builder);
              return builder.build();
            } catch (InvalidProtocolBufferException e) {
              throw new RuntimeException(e);
            }
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        })
        .forEach(toolBuilder::addFunctionDeclarations);
    return List.of(toolBuilder.build());
  }

  @Override
  public GenericChatMessage chat(String message, Context context) {
    ChatSession<Content, FunctionCall> session = new ChatSession<>(backend, context, systemPrompt, bindings);
    Content chatMessage = ContentMaker.fromString(message);

    int retryCount = 0;
    while (true) {
      ContextWindow<Content> contextWindow = session.getContextWindow();
      com.google.cloud.vertexai.generativeai.ChatSession chatSession = chatModel.startChat();
      List<Content> messageHistory = contextWindow.getMessages().stream().filter(m -> !m.getRole().equals("system")).toList();
      chatSession.setHistory(messageHistory);

      log.info("Calling Google Vertex with model {}", chatModel.getModelName());
      log.debug("and message {}", chatMessage);
      ModelInvocation invocation = observability.start();
      context.nextInvocation();
      try {
        GenerateContentResponse generatedResponse = chatSession.sendMessage(chatMessage);
        Content response = ResponseHandler.getContent(generatedResponse);
        invocation.stop(contextWindow.getNumTokens(), bindings.getTokenCounter().countTokens(response));
        log.debug("Response:\n{}", generatedResponse);
        session.addMessage(chatMessage);
        GenericChatMessage genericResponse = session.addMessage(response);
        Optional<FunctionCall> functionCall = response.getPartsList().stream().filter(Part::hasFunctionCall).map(Part::getFunctionCall).findFirst();
        if (functionCall.isPresent()) {
          ChatSession.FunctionExecutionOutcome<Content> outcome = session.validateAndExecuteFunctionCall(functionCall.get(), false);
          switch (outcome.status()) {
            case EXECUTE_ON_CLIENT -> {
              return genericResponse;
            }
            case EXECUTED -> {
              chatMessage = outcome.functionResponse();
            }
            case VALIDATION_ERROR_RETRY -> {
              if (retryCount >= AbstractChatProvider.FUNCTION_CALL_RETRIES_LIMIT) {
                throw new RuntimeException("Too many function call retries for the same function.");
              } else {
                retryCount++;
                log.debug("Failed function call: {}", functionCall);
                log.info("Function call failed. Retrying ...");
                chatMessage = outcome.functionResponse();
              }
            }
          }
        } else {
          //The text answer
          return genericResponse;
        }
      } catch (IOException e) {
        invocation.fail(e);
        throw new RuntimeException(e);
      }
    }
  }

  private void capitalizeObjectTypes(JsonNode node) {
    if (node.isObject()) {
      ObjectNode objectNode = (ObjectNode) node;
      // Iterate over the fields of the object node
      objectNode.fieldNames().forEachRemaining((String fieldName) -> {
        JsonNode childNode = objectNode.get(fieldName);
        if ("type".equals(fieldName) && childNode.isTextual()) {
          String capsType = childNode.textValue().toUpperCase();
          objectNode.put(fieldName, capsType);
        } else {
          capitalizeObjectTypes(childNode);
        }
      });
    } else if (node.isArray()) {
      // If the node is an array, iterate over the elements
      for (JsonNode arrayElement : node) {
        capitalizeObjectTypes(arrayElement);
      }
    }
  }

}
