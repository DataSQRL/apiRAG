package com.datasqrl.ai.function;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import javax.annotation.Nonnull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@FunctionDescription("Adds two numbers")
public class TestAddFunction implements UserDefinedFunction {

  @JsonPropertyDescription("The first argument")
  @Nonnull
  public Number arg1;

  @JsonPropertyDescription("The second argument")
  @Nonnull
  public Number arg2;

  @Override
  public Object execute() {
    return arg1.doubleValue() + arg2.doubleValue();
  }
}
