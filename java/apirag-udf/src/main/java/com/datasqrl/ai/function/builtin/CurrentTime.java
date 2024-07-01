package com.datasqrl.ai.function.builtin;

import com.datasqrl.ai.function.FunctionDescription;
import com.datasqrl.ai.function.UserDefinedFunction;
import java.time.Instant;

@FunctionDescription("Current date and time in ISO 8601 format")
public class CurrentTime implements UserDefinedFunction {

  @Override
  public String execute() {
    return Instant.now().toString();
  }
}
