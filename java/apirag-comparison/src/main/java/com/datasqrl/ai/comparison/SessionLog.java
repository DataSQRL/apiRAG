package com.datasqrl.ai.comparison;

import java.util.List;

public record SessionLog(List<LogEntry> entries) {

  public record LogEntry(String query, String expectedResponse, String actualResponse,
                         TestChatSession.ChatQuery.AnswerType expectedAnswerType,
                         TestChatSession.ChatQuery.AnswerType actualAnswerType) {
  }
}
