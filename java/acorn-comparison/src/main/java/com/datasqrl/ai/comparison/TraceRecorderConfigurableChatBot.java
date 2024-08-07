package com.datasqrl.ai.comparison;

import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.APIExecutorFactory;
import com.datasqrl.ai.api.GraphQLExecutorFactory;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.ModelObservability;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.tool.ToolManager;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.ToolsBackendFactory;
import com.datasqrl.ai.trace.Trace;
import com.datasqrl.ai.trace.TraceChatProvider;
import com.datasqrl.ai.trace.TraceContext;
import com.datasqrl.ai.trace.TraceRecordingToolManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.MapConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * A simple chatbot for the command line.
 * Uses a fully contained ChatProvider, configured manually.
 */
@Slf4j
@Value
public class TraceRecorderConfigurableChatBot {

  ChatProvider chatProvider;

  /**
   * Initializes a command line chatbot
   *
   * @param chatProvider    The ChatProvider to take care of the communication with the LLM and the function execution
   */
  public TraceRecorderConfigurableChatBot(ChatProvider chatProvider) {
    this.chatProvider = chatProvider;
  }

  /**
   * Starts the chatbot on the command line which will accepts questions and producez responses.
   * Type "exit" to terminate.
   *
   * @param context        The user context that might be needed to execute functions
   */
  public void start(TraceContext context) throws IOException {
    Scanner scanner = new Scanner(System.in);
    System.out.print("First Query: ");
    String message = scanner.nextLine();
    while (true) {
      GenericChatMessage response = chatProvider.chat(message, context);
      context = context.nextRequest();
      System.out.println("Response: " + response.getContent());
      System.out.print("Next Query: ");
      String nextLine = scanner.nextLine();
      if (nextLine.equalsIgnoreCase("exit")) {
        break;
      }
      message = nextLine;
    }
  }

  private static void writeToFile(Trace trace, String fileName) {
    ObjectMapper mapper = new ObjectMapper();
    File file = new File(fileName);
    try (FileWriter fileWriter = new FileWriter(file, true)) {
      fileWriter.write(mapper.writeValueAsString(trace));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String... args) throws Exception {
    String systemPrompt = "You are a huge fan of the Ricky and Morty TV show and help users answer questions about the show. You always try to look up the information a user is asking for via one of the available functions. Only if you cannot find the information do you use general knowledge to answer it. Retrieved information always takes precedence. You answer in the voice of Jerry Smith.";
    URL toolsResource = TraceRecorderConfigurableChatBot.class.getClassLoader().getResource("tools/rickandmorty.tools.json");
    List<RuntimeFunctionDefinition> tools = ToolsBackendFactory.readTools(toolsResource);
    APIExecutor apiExecutor = new GraphQLExecutorFactory().create(new MapConfiguration(Map.of(
        "type", "graphql",
        "url", "https://rickandmortyapi.com/graphql"
    )), "rickandmortyapi");
    ToolsBackend backend = ToolsBackendFactory.of(tools, Map.of(APIExecutorFactory.DEFAULT_NAME, apiExecutor));
    Trace.TraceBuilder traceBuilder = Trace.builder();
    ToolManager toolsBackend = new TraceRecordingToolManager(backend, traceBuilder, Optional.empty());

    ChatProvider provider = ChatProviderFactory.fromConfiguration(Map.of(
        "provider", "openai",
        "name", "gpt-4o-mini",
        "temperature", 0.2,
        "max_output_tokens", 2048
    ), toolsBackend, systemPrompt, ModelObservability.NOOP);
    ChatProvider chatProvider = new TraceChatProvider(provider, traceBuilder);

    TraceRecorderConfigurableChatBot chatBot = new TraceRecorderConfigurableChatBot(chatProvider);
    chatBot.start(TraceContext.of());
    Trace trace = traceBuilder.build();
    writeToFile(trace, "trace.json");
  }

}