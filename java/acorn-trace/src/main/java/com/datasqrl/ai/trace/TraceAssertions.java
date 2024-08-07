package com.datasqrl.ai.trace;

import com.datasqrl.ai.trace.Trace.FunctionCall;
import com.datasqrl.ai.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TraceAssertions {

  private static final Map<Boolean, TraceEvaluator<TraceComparisonResult>> COMPARISONS = Map.of(
    true, TraceEvaluator.equalityOnly(true),
    false, TraceEvaluator.equalityOnly(false)
  );

  public static void assertContainsIgnoreCase(String message, String... words) {
    TraceEvaluator.containsBagOfWords(message, words).assertCorrect();
  }

  public static void assertEquals(FunctionCall expected, FunctionCall given, boolean strict) {
    COMPARISONS.get(strict).compare(expected, given).assertCorrect();
  }

  public static void assertEquals(Map<String, Object> expectedArguments, JsonNode givenArguments, boolean strict) {
    COMPARISONS.get(strict).compare(JsonUtil.convert(expectedArguments), givenArguments).assertCorrect();
  }

  public static void assertSameFunctionCalls(Iterable<FunctionCall> expectedCalls, Trace given, boolean strict) {
    for (FunctionCall expectedCall : expectedCalls) {
      FunctionCall givenCall = given.getFunctionCall(expectedCall.requestId(),
          expectedCall.invocationId());
      if (givenCall==null) throw new AssertionError("Could not find function call: " + expectedCall);
      assertEquals(expectedCall, givenCall, strict);
    }
  }


}
