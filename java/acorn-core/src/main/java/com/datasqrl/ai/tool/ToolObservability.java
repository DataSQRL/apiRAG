package com.datasqrl.ai.tool;

public interface ToolObservability {

  ToolCall start(String toolName);

  interface ToolCall {

    void stop();

    void fail(Exception e);
  }


}
