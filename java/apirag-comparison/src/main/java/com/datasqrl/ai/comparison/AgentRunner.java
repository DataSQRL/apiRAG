package com.datasqrl.ai.comparison;

import com.datasqrl.ai.backend.GenericChatMessage;
import com.datasqrl.ai.comparison.config.ComparisonConfiguration;
import com.datasqrl.ai.models.ChatClientProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.text.SimpleDateFormat;

import static com.datasqrl.ai.comparison.config.ComparisonConfiguration.MODEL_PREFIX;
import static com.datasqrl.ai.comparison.config.ComparisonConfiguration.MODEL_PROVIDER_KEY;

@Slf4j
public class AgentRunner {

  private final List<TestChatSession> testSessions;
  private final ChatClientProvider chatProvider;
  private final Function<String, Map<String, Object>> contextFunction;
  private final String modelName;
  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  private final ObjectMapper mapper = new ObjectMapper();

  public AgentRunner(ComparisonConfiguration configuration, List<TestChatSession> testChatSessions) {
    this.testSessions = testChatSessions;
    contextFunction = configuration.getContextFunction();
    chatProvider = configuration.getChatProvider();
    modelName = configuration.getModelConfiguration().getString(MODEL_PROVIDER_KEY) + "-" + configuration.getModelConfiguration().getString(MODEL_PREFIX);
  }

  public void run() {
    AtomicInteger idCounter = new AtomicInteger(1);
    String fileName = modelName + "-Id" + idCounter.get() + "-" + getCurrentTime() + ".json";
    writeToFile("[\n", fileName);
    testSessions.forEach(session -> {
      log.info("Running session with userId: {}", idCounter.get());
      Map<String, Object> context = contextFunction.apply(Integer.toString(idCounter.getAndIncrement()));
      SessionLog sessionLog = runChatSession(session, context);
      writeToFile(sessionLog, fileName);
    });
    writeToFile("\n]", fileName);
  }

  private void writeToFile(String s, String fileName) {
    File file = new File(fileName);
    try (FileWriter fileWriter = new FileWriter(file, true)) {
      fileWriter.write(s);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeToFile(SessionLog sessionLog, String fileName) {
    File file = new File(fileName);
    try (FileWriter fileWriter = new FileWriter(file, true)) {
      SequenceWriter seqWriter = mapper.writer().writeValuesAsArray(fileWriter);
      seqWriter.write(sessionLog);
      seqWriter.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getCurrentTime() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    return sdf.format(timestamp);
  }

  private SessionLog runChatSession(TestChatSession session, Map<String, Object> context) {
    List<SessionLog.LogEntry> sessions = new ArrayList<>();
    session.queries().forEach(query -> {
      log.info("Query: {}", query.query());
      GenericChatMessage response = chatProvider.chat(query.query(), context);
      log.info("Response: {}", response.getContent());
      sessions.add(logInteraction(query, response));
    });
    SessionLog logs = new SessionLog(sessions);
    log.info("SessionLog: {}", logs);
    return logs;
  }

  private SessionLog.LogEntry logInteraction(TestChatSession.ChatQuery query, GenericChatMessage response) {
    TestChatSession.ChatQuery.AnswerType answerType =
        response.getFunctionCall() == null ? TestChatSession.ChatQuery.AnswerType.TEXT : TestChatSession.ChatQuery.AnswerType.FUNCTION_CALL;
    return new SessionLog.LogEntry(query.query(), query.expectedAnswer(), response.getContent(), query.expectedAnswerType(), answerType);
  }
}
