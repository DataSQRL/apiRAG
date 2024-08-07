package com.datasqrl.ai.models.openai;

import com.datasqrl.ai.models.AbstractChatProvider;
import com.datasqrl.ai.models.ChatSession;
import com.datasqrl.ai.models.ContextWindow;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.ModelObservability;
import com.datasqrl.ai.tool.ModelObservability.ModelInvocation;
import com.datasqrl.ai.tool.ToolManager;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.datasqrl.ai.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenAiChatProvider extends AbstractChatProvider<ChatMessage, ChatFunctionCall> {

  private final OpenAIModelConfiguration config;
  private final OpenAiService service;
  private final String systemPrompt;

  public OpenAiChatProvider(OpenAIModelConfiguration config, ToolManager backend, String systemPrompt, ModelObservability observability) {
    super(backend, new OpenAIModelBindings(config), observability);
    this.config = config;
    this.systemPrompt = systemPrompt;
    String openAIToken = ConfigurationUtil.getEnvOrSystemVariable("OPENAI_API_KEY");
    this.service = new OpenAiService(openAIToken, Duration.ofSeconds(60));
  }

  @Override
  public GenericChatMessage chat(String message, Context context) {
    ChatSession<ChatMessage, ChatFunctionCall> session = new ChatSession<>(backend, context, systemPrompt, bindings);
    ChatMessage chatMessage = new UserMessage(message);
    session.addMessage(chatMessage);

    int retryCount = 0;
    while (true) {
      log.info("Calling OpenAI with model {}", config.getModelName());
      ContextWindow<ChatMessage> contextWindow = session.getContextWindow();
      log.debug("Calling OpenAI with messages: {}", contextWindow.getMessages());
      ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
          .builder()
          .model(config.getModelName())
          .messages(contextWindow.getMessages())
          .functions(contextWindow.getFunctions())
          .n(1)
          .topP(config.getTopP())
          .temperature(config.getTemperature())
          .maxTokens(config.getMaxOutputTokens())
          .logitBias(new HashMap<>())
          .build();
      ModelInvocation invocation = observability.start();
      context.nextInvocation();
      AssistantMessage responseMessage = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage();
      invocation.stop(contextWindow.getNumTokens(), bindings.getTokenCounter().countTokens(responseMessage));
      log.debug("Response:\n{}", responseMessage);
      String res = responseMessage.getTextContent();
      // Workaround for openai4j who doesn't recognize some function calls
      if (res != null) {
        String responseText = res.trim();
        if (responseText.startsWith("{\"function\"") && responseMessage.getFunctionCall() == null) {
          ChatFunctionCall functionCall = getFunctionCallFromText(responseText).orElse(null);
          if (functionCall != null) {
          responseMessage = new AssistantMessage("", functionCall.getName(), null, functionCall);
          log.info("!!!Remapped content to function call");
          }
        }
      }
      GenericChatMessage genericResponse = session.addMessage(responseMessage);
      ChatFunctionCall functionCall = responseMessage.getFunctionCall();
      if (functionCall != null) {
        ChatSession.FunctionExecutionOutcome<ChatMessage> outcome = session.validateAndExecuteFunctionCall(functionCall, true);
        switch (outcome.status()) {
          case EXECUTE_ON_CLIENT -> {
            return genericResponse;
          }
          case VALIDATION_ERROR_RETRY -> {
            if (retryCount >= AbstractChatProvider.FUNCTION_CALL_RETRIES_LIMIT) {
              throw new RuntimeException("Too many function call retries for the same function.");
            } else {
              retryCount++;
              log.debug("Failed function call: {}", functionCall);
              log.info("Function call failed. Retrying ...");
            }
          }
        }
      } else {
        // The text answer
        return genericResponse;
      }
    }
  }

  private static Optional<ChatFunctionCall> getFunctionCallFromText(String text) {
    Optional<JsonNode> functionCall = JsonUtil.parseJson(text);
    if (functionCall.isEmpty()) {
      log.error("Could not parse function text [{}]:\n", text);
      return Optional.empty();
    } else {
      return functionCall.map(json -> new ChatFunctionCall(json.get("function").asText(), json.get("parameters")));
    }
  }

}
