package com.datasqrl.ai.comparison;

import com.datasqrl.ai.comparison.config.ComparisonConfiguration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Value
public class ComparisonRunner {

  List<String> modelFiles;
  List<String> useCaseFolders;
  String scriptFile;
  ObjectMapper mapper = new ObjectMapper();

  public ComparisonRunner(List<String> modelFiles, List<String> useCaseFolders, String scriptFile) {
    this.modelFiles = modelFiles;
    this.useCaseFolders = useCaseFolders;
    this.scriptFile = scriptFile;
  }

  public void start() throws IOException {
    modelFiles.forEach(modelConfig -> {
      log.info("Loading model config from {}", modelConfig);
      useCaseFolders.forEach(useCaseFolder -> {
      log.info("Loading use case from {}", useCaseFolder);
        Optional<Path> useCaseConfig;
        Optional<Path> tools;
        Path useCaseDir = Paths.get(useCaseFolder);
        try (Stream<Path> stream = Files.list(useCaseDir)) {
          useCaseConfig = stream.filter(path -> !Files.isDirectory(path)).filter(path -> path.getFileName().toString().endsWith(".config.json")).findFirst();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        try (Stream<Path> stream2 = Files.list(useCaseDir)) {
          tools = stream2.filter(path -> !Files.isDirectory(path)).filter(path -> path.getFileName().toString().endsWith(".tools.json")).findFirst();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        if (useCaseConfig.isPresent() && tools.isPresent()) {
          ComparisonConfiguration configuration = ComparisonConfiguration.fromFile(Path.of(modelConfig), useCaseConfig.get(), tools.get());
          List<TestChatSession> testSessions = loadTestChatSessionsFromFile(scriptFile);
          log.info("Loaded {} test sessions from {}", testSessions.size(), scriptFile);
          new AgentRunner(configuration, testSessions).run();
        } else {
          log.error("Could not load configuration and UseCase config from folder {}", useCaseFolder);
        }
      });
    });
  }

  private List<TestChatSession> loadTestChatSessionsFromFile(String fileName) {
    try {
      return mapper.readValue(Paths.get(fileName).toFile(), new TypeReference<List<TestChatSession>>() {
      });
    } catch (Exception ex) {
      log.error("Could not read test chat sessions from {}", fileName, ex);
    }
    return List.of();
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