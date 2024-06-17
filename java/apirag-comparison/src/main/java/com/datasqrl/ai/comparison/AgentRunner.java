package com.datasqrl.ai.comparison;

import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.GenericChatMessage;
import com.datasqrl.ai.comparison.config.ComparisonConfiguration;
import com.datasqrl.ai.models.ChatClientProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AgentRunner {

  private final List<TestChatQuery> queries;
  private final ChatClientProvider chatProvider;

  public AgentRunner(ComparisonConfiguration configuration, List<TestChatQuery> queries) {
    this.queries = queries;
    FunctionBackend functionBackend = configuration.getFunctionBackend();
    Function<String, Map<String, Object>> contextFunction = configuration.getContextFunction();
    chatProvider = configuration.getChatProvider();
  }

  public void run() {
    queries.stream().forEach(query -> {
      executeQuery(query.query(), query.expectedAnswer(), this.getContext());
    });
  }

  private Map<String, Object> getContext() {
    return null;
  }

  private void executeQuery(String query, String s, Map<String, Object> context) {
    GenericChatMessage answer = chatProvider.chat(query, context);
    evaluateAnswer(query, answer);
  }

  private void evaluateAnswer(String query, GenericChatMessage answer) {

  }
}
