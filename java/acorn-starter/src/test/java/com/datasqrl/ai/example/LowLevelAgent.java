package com.datasqrl.ai.example;

import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.APIExecutorFactory;
import com.datasqrl.ai.api.RESTExecutorFactory;
import com.datasqrl.ai.models.ChatSession;
import com.datasqrl.ai.models.ContextWindow;
import com.datasqrl.ai.models.ModelAnalyzer;
import com.datasqrl.ai.models.openai.OpenAIModelBindings;
import com.datasqrl.ai.models.openai.OpenAIModelConfiguration;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.FunctionValidation;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.ToolsBackendFactory;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.FunctionMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import java.net.URL;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.MapConfiguration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An example of a streaming chatbot for the command line, which is using the low level components of the AcornAgent Framework.
 * All API and model configuration is done in the main() function.
 * The system prompt is hardcoded and the tools are loaded from file.
 */
@Slf4j
@Value
public class LowLevelAgent {

  OpenAIModelConfiguration chatConfig;
  String systemPrompt;
  OpenAiService service;
  ToolsBackend toolsBackend;


  /**
   * Initializes a command line chatbot using OpenAI
   *
   * @param toolsBackend The tools backend to use for function execution.
   * @param chatConfig   The openAI model configuration
   * @param systemPrompt The system prompt for the LLM
   * @param openAIKey    The OpenAI API key to call the API
   */

  public LowLevelAgent(ToolsBackend toolsBackend, OpenAIModelConfiguration chatConfig, String systemPrompt, String openAIKey) {
    this.toolsBackend = toolsBackend;
    this.chatConfig = chatConfig;
    this.systemPrompt = systemPrompt;
    this.service = new OpenAiService(openAIKey, Duration.ofSeconds(60));
  }

  /**
   * Starts the chatbot on the command line which will accepts questions and produce responses.
   * Type "exit" to terminate.
   *
   * @param context        The user context that might be needed to execute functions
   */
  public void start(Context context) {
    Scanner scanner = new Scanner(System.in);
    OpenAIModelBindings modelBindings = new OpenAIModelBindings(chatConfig);
    ChatSession<ChatMessage, ChatFunctionCall> session = new MyChatSession(toolsBackend, context, systemPrompt, modelBindings);

    System.out.print("First Query: ");
    ChatMessage firstMsg = new UserMessage(scanner.nextLine());
    session.addMessage(firstMsg);

    while (true) {
      ContextWindow<ChatMessage> contextWindow = session.getContextWindow();
      ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
          .builder()
          .model(chatConfig.getModelName())
          .messages(contextWindow.getMessages())
          .functions(contextWindow.getFunctions())
          .n(1)
          .topP(chatConfig.getTopP())
          .temperature(chatConfig.getTemperature())
          .maxTokens(chatConfig.getMaxOutputTokens())
          .logitBias(new HashMap<>())
          .build();
      context.nextInvocation();
      Flowable<ChatCompletionChunk> flowable = service.streamChatCompletion(chatCompletionRequest);

      AtomicBoolean isFirst = new AtomicBoolean(true);
      AssistantMessage responseMessage = service.mapStreamToAccumulator(flowable)
          .doOnNext(accumulator -> {
            if (accumulator.isFunctionCall()) {
              if (isFirst.getAndSet(false)) {
                System.out.println("Executing function " + accumulator.getAccumulatedChatFunctionCall().getName() + "...");
              }
            } else {
              if (isFirst.getAndSet(false)) {
                System.out.print("Response: ");
              }
              if (accumulator.getMessageChunk().getContent() != null) {
                System.out.print(accumulator.getMessageChunk().getContent());
              }
            }
          })
          .doOnComplete(System.out::println)
          .lastElement()
          .blockingGet()
          .getAccumulatedMessage();
      session.addMessage(responseMessage);

      ChatFunctionCall functionCall = responseMessage.getFunctionCall();
      if (functionCall != null) {
        //        Basic function validation without retries
        FunctionValidation<ChatMessage> functionValidation = session.validateFunctionCall(functionCall);
        if (functionValidation.isValid()) {
          log.info("Executing {} with arguments {}", functionCall.getName(), functionCall.getArguments().toPrettyString());
          ChatMessage functionResponse = session.executeFunctionCall(functionCall, context);
          log.info("Executed {} with results: {}", functionCall.getName(), functionResponse.getTextContent());
          session.addMessage(functionResponse);
        } else {
          log.info("Function call {} failed.", functionCall.getName());
          session.addMessage(new FunctionMessage("{\"error\": \"" + functionValidation.validationError().errorMessage() + "\"}", "error"));
        }
      } else {
        System.out.print("Next Query: ");
        String nextLine = scanner.nextLine();
        if (nextLine.equalsIgnoreCase("exit")) {
          System.exit(0);
        }
        ChatMessage nextMsg = new UserMessage(nextLine);
        session.addMessage(nextMsg);
      }
    }
  }

  class MyChatSession extends ChatSession<ChatMessage, ChatFunctionCall> {

    public MyChatSession(ToolsBackend toolsBackend, Context context, String systemPrompt, OpenAIModelBindings modelBindings) {
      super(toolsBackend, context, systemPrompt, modelBindings);
    }

    // Custom implementation of a ChatSession method in order to control the size of the context window, which can be costly
    @Override
    protected ContextWindow<GenericChatMessage> getContextWindow(int maxTokens, ModelAnalyzer<ChatMessage> analyzer) {
      GenericChatMessage systemMessage = this.bindings.convertMessage(this.bindings.createSystemMessage(systemPrompt), this.context);
      final AtomicInteger numTokens = new AtomicInteger(0);
      //      Count tokens for system prompt
      numTokens.addAndGet(systemMessage.getNumTokens());
      ContextWindow.ContextWindowBuilder<GenericChatMessage> builder = ContextWindow.builder();
      //      Count tokens for functions
      this.backend.getFunctions().values().stream()
          .map(RuntimeFunctionDefinition::getChatFunction)
          .peek(f -> numTokens.addAndGet(analyzer.countTokens(f)))
          .forEach(builder::function);
      if (numTokens.get() > maxTokens)
        throw new IllegalArgumentException("Function calls and system message too large for model: " + numTokens);
      int numMessages = messages.size();
      List<GenericChatMessage> contextMessages = new ArrayList<>();
      ListIterator<GenericChatMessage> listIterator = messages.listIterator(numMessages);
      //      Allow maximum 3 past messages in context window
      while (listIterator.hasPrevious() && contextMessages.size() < 3) {
        GenericChatMessage message = listIterator.previous();
        numTokens.addAndGet(message.getNumTokens(msg -> analyzer.countTokens(bindings.convertMessage(msg))));
        if (numTokens.get() > maxTokens) break;
        contextMessages.add(message);
        numMessages--;
      }
      builder.message(systemMessage);
      Collections.reverse(contextMessages);
      builder.messages(contextMessages);
      builder.numTokens(numTokens.get());
      ContextWindow<GenericChatMessage> window = builder.build();
      if (numMessages > 0) log.info("Truncated the first {} messages", numMessages);
      return window;
    }
  }


  public static void main(String... args) throws Exception {
    String systemPrompt = "You are an incredibly enthusiastic and joyous life coach. You provide advice to people. You look up any and all advice you give via the provided functions.";
    URL toolsResource = LowLevelAgent.class.getClassLoader().getResource("tools/advice.tools.json");
    APIExecutor apiExecutor = new RESTExecutorFactory().create(new MapConfiguration(Map.of(
        "type", "rest",
        "url", "https://api.adviceslip.com"
    )), "adviceapi");
    List<RuntimeFunctionDefinition> tools = ToolsBackendFactory.readTools(toolsResource);
    ToolsBackend toolsBackend = ToolsBackendFactory.of(tools, Map.of(APIExecutorFactory.DEFAULT_NAME, apiExecutor));
    String openaiKey = ConfigurationUtil.getEnvOrSystemVariable("OPENAI_API_KEY");
    OpenAIModelConfiguration chatConfig = new OpenAIModelConfiguration(
        new MapConfiguration(Map.of(
            "name", "gpt-4o-mini",
            "temperature", 0.3)));

    LowLevelAgent agent = new LowLevelAgent(toolsBackend, chatConfig, systemPrompt, openaiKey);
    agent.start(Context.of());
  }
}