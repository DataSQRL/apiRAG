package com.datasqrl.ai.example;

import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.APIExecutorFactory;
import com.datasqrl.ai.api.GraphQLExecutorFactory;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.ModelObservability;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.ToolsBackendFactory;
import java.net.URL;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.MapConfiguration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * A simple chatbot for the command line.
 * Uses a fully contained ChatProvider, configured manually.
 */
@Slf4j
@Value
public class ConfigurableChatBot {

  ChatProvider chatProvider;

  /**
   * Initializes a command line chatbot
   *
   * @param chatProvider    The ChatProvider to take care of the communication with the LLM and the function execution
   */
  public ConfigurableChatBot(ChatProvider chatProvider) {
    this.chatProvider = chatProvider;
  }

  /**
   * Starts the chatbot on the command line which will accepts questions and producez responses.
   * Type "exit" to terminate.
   *
   * @param context        The user context that might be needed to execute functions
   */
  public void start(Context context) throws IOException {
    Scanner scanner = new Scanner(System.in);
    System.out.print("First Query: ");
    String message = scanner.nextLine();
    while (true) {
      GenericChatMessage response = chatProvider.chat(message, context);
      System.out.println("Response: " + response.getContent());
      System.out.print("Next Query: ");
      String nextLine = scanner.nextLine();
      if (nextLine.equalsIgnoreCase("exit")) {
        System.exit(0);
      }
      message = nextLine;
    }
  }

  public static void main(String... args) throws Exception {
    String systemPrompt = "You are a huge fan of the Ricky and Morty TV show and help users answer questions about the show. You always try to look up the information a user is asking for via one of the available functions. Only if you cannot find the information do you use general knowledge to answer it. Retrieved information always takes precedence. You answer in the voice of Jerry Smith.";
    URL toolsResource = ConfigurableChatBot.class.getClassLoader().getResource("tools/rickandmorty.tools.json");
    List<RuntimeFunctionDefinition> tools = ToolsBackendFactory.readTools(toolsResource);
    APIExecutor apiExecutor = new GraphQLExecutorFactory().create(new MapConfiguration(Map.of(
        "type", "graphql",
        "url", "https://rickandmortyapi.com/graphql"
    )), "rickandmortyapi");
    ToolsBackend toolsBackend = ToolsBackendFactory.of(tools, Map.of(APIExecutorFactory.DEFAULT_NAME, apiExecutor));
    ChatProvider chatProvider = ChatProviderFactory.fromConfiguration(Map.of(
        "provider", "groq",
        "name", "mixtral-8x7b-32768",
        "temperature", 0.8
    ), toolsBackend, systemPrompt, ModelObservability.NOOP);

    ConfigurableChatBot chatBot = new ConfigurableChatBot(chatProvider);
    chatBot.start(Context.of());
  }

}