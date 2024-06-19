package com.datasqrl.ai;

import com.datasqrl.ai.backend.ChatSession;
import com.datasqrl.ai.backend.ContextWindow;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.config.DataAgentConfiguration;
import com.datasqrl.ai.models.openai.OpenAIModelBindings;
import com.datasqrl.ai.models.openai.OpenAiChatModel;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.FunctionMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import lombok.Value;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple streaming chatbot for the command line.
 * The implementation uses OpenAI's GPT models with a default configuration
 * and {@link FunctionBackend} to call APIs that pull in requested data
 * as well as save and restore chat messages across sessions.
 *
 * This implementation is based on <a href="https://github.com/TheoKanning/openai-java/blob/main/example/src/main/java/example/OpenAiApiFunctionsWithStreamExample.java">https://github.com/TheoKanning/openai-java</a>
 * and meant only for demonstration and testing.
 *
 * To run the main method, you need to set your OPENAI token as an environment variable.
 * The main method expects two arguments: A configuration file and a tools file.
 */
@Value
public class CmdLineChatBot {

  OpenAiService service;
  FunctionBackend backend;
  OpenAiChatModel chatModel = OpenAiChatModel.GPT35_TURBO;

  /**
   * Initializes a command line chat bot
   *
   * @param openAIKey The OpenAI API key to call the API
   * @param backend An initialized backend to use for function execution and chat message persistence
   */
  public CmdLineChatBot(String openAIKey, FunctionBackend backend) {
    service = new OpenAiService(openAIKey, Duration.ofSeconds(60));
    this.backend = backend;
  }

  /**
   * Starts the chatbot on the command line which will accepts questions and produce responses.
   * Type "exit" to terminate.
   *
   * @param instructionMessage The system instruction message for the ChatBot
   */
  public void start(String instructionMessage, Map<String, Object> context) throws IOException {
    Scanner scanner = new Scanner(System.in);
    OpenAIModelBindings modelBindings = new OpenAIModelBindings(chatModel);
    ChatSession<ChatMessage, ChatFunctionCall> session = new ChatSession<>(backend, context, instructionMessage, modelBindings);

    System.out.print("First Query: ");
    ChatMessage firstMsg = new UserMessage(scanner.nextLine());
    session.addMessage(firstMsg);

    while (true) {
      ContextWindow<ChatMessage> contextWindow = session.getContextWindow();
      ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
          .builder()
          .model(chatModel.getModelName())
          .messages(contextWindow.getMessages())
          .functions(contextWindow.getFunctions())
          .functionCall("auto")
          .n(1)
          .maxTokens(chatModel.getContextWindowLength())
          .logitBias(new HashMap<>())
          .build();
      Flowable<ChatCompletionChunk> flowable = service.streamChatCompletion(chatCompletionRequest);

      AtomicBoolean isFirst = new AtomicBoolean(true);
      AssistantMessage chatMessage = service.mapStreamToAccumulator(flowable)
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
      session.addMessage(chatMessage);

      if (chatMessage.getFunctionCall() != null) {
        ChatFunctionCall fctCall = chatMessage.getFunctionCall();
        FunctionValidation<String> fctValid = backend.validateFunctionCall(fctCall.getName(), fctCall.getArguments());
        if (fctValid.isValid()) {
          System.out.println("Trying to execute " + fctCall.getName() + " with arguments " + fctCall.getArguments().toPrettyString());
          ChatMessage functionResponse = new FunctionMessage(backend.executeFunctionCall(fctCall.getName(), fctCall.getArguments(), context), fctCall.getName());
          System.out.println("Executed " + fctCall.getName() + " with response: " + functionResponse.getTextContent());
          session.addMessage(functionResponse);
        } else {
          session.addMessage(new FunctionMessage("{\"error\": \"" + fctValid.validationError().errorMessage() + "\"}", "error"));
        }
        continue;
      }

      System.out.print("Next Query: ");
      String nextLine = scanner.nextLine();
      if (nextLine.equalsIgnoreCase("exit")) {
        System.exit(0);
      }
      ChatMessage nextMsg = new UserMessage(nextLine);
      session.addMessage(nextMsg);
    }
  }

  public static void main(String... args) throws Exception {
    if (args==null || args.length!=2) throw new IllegalArgumentException("Please provide a configuration file and a tools file");
    DataAgentConfiguration configuration = DataAgentConfiguration.fromFile(Path.of(args[0]), Path.of(args[1]), null);


    Map<String,Object> context = Map.of();
    if (configuration.hasAuth()) {
      Scanner scanner = new Scanner(System.in);
      System.out.print("Enter the User ID: ");
      String userid = scanner.nextLine();
      context = configuration.getContextFunction().apply(userid);
    }

    FunctionBackend backend = configuration.getFunctionBackend();
    String openAIToken = System.getenv("OPENAI_API_KEY");
    CmdLineChatBot chatBot = new CmdLineChatBot(openAIToken, backend);
    chatBot.start(configuration.getSystemPrompt(), context);
  }

}