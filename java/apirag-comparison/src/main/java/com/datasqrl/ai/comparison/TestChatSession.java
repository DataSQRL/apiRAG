package com.datasqrl.ai.comparison;

import java.util.List;

public record TestChatSession(List<ChatQuery> queries) {

  public record ChatQuery(String query, String expectedAnswer, AnswerType expectedAnswerType) {

    public enum AnswerType {
      TEXT, FUNCTION_CALL
    }
  }

}
