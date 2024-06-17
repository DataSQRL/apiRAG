package com.datasqrl.ai.comparison;

import com.datasqrl.ai.backend.ChatSession;
import com.datasqrl.ai.backend.ContextWindow;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.comparison.config.ComparisonConfiguration;
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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
@Slf4j
@Value
public class ComparisonRunner {

  private final List<String> modelFiles;
  private final List<String> useCaseFolders;
  private final String scriptFile;

  public ComparisonRunner(List<String> modelFiles, List<String> useCaseFolders, String scriptFile) {
    this.modelFiles = modelFiles;
    this.useCaseFolders = useCaseFolders;
    this.scriptFile = scriptFile;
  }

  public void start() throws IOException {
    modelFiles.forEach(modelConfig -> {
      useCaseFolders.forEach(useCaseFolder -> {
        Optional<Path> useCaseConfig = Optional.empty();
        Optional<Path> tools = Optional.empty();
        try (Stream<Path> stream = Files.list(Paths.get(useCaseFolder))) {
          useCaseConfig = stream.filter(path -> !Files.isDirectory(path)).filter(path -> path.getFileName().toString().endsWith(".config.json")).findFirst();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        try (Stream<Path> stream2 = Files.list(Paths.get(useCaseFolder))) {
          tools = stream2.filter(path -> !Files.isDirectory(path)).filter(path -> path.getFileName().toString().endsWith(".tools.json")).findFirst();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        if (useCaseConfig.isPresent() && tools.isPresent()) {
          ComparisonConfiguration configuration = ComparisonConfiguration.fromFile(Path.of(modelConfig), useCaseConfig.get(), tools.get());
          new AgentRunner(configuration, scriptFile).run();
        } else {
          log.error("Could not load configuration and UseCase config from folder {}", useCaseFolder);
        }
      });
    });
  }

public static void main(String... args) throws Exception {
  if (args == null || args.length != 3)
    throw new IllegalArgumentException("Please provide a models folder, a use case folder and a script file");
  List<String> modelFiles;
  List<String> useCaseFolders;
  String scriptFile = args[2];
  try (Stream<Path> stream = Files.list(Paths.get(args[0]))) {
    modelFiles = stream
        .filter(file -> !Files.isDirectory(file))
        .map(Path::getFileName)
        .map(Path::toString)
        .toList();
  }
  try (Stream<Path> stream = Files.list(Paths.get(args[1]))) {
    useCaseFolders = stream
        .filter(Files::isDirectory)
        .map(Path::getFileName)
        .map(Path::toString)
        .toList();
  }


  ComparisonRunner runner = new ComparisonRunner(modelFiles, useCaseFolders, scriptFile);
  runner.start();
}

}