package com.datasqrl.ai.trace;

import com.datasqrl.ai.trace.QualitativeTraceJudge.QualitativeResult;
import com.datasqrl.ai.trace.Trace.Entry;
import com.datasqrl.ai.trace.Trace.FunctionCall;
import com.datasqrl.ai.trace.Trace.Judgement;
import com.datasqrl.ai.trace.Trace.Response;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class TraceEvaluator<R extends TraceComparisonResult> {

  public static final Double DELTA = 0.0001;
  public static final String DEFAULT_DELIMITER = "\\s+";

  boolean strict;
  TraceJudge<R> judge;
  double delta;
  String delimiter;

  public TraceEvaluator(boolean strict, TraceJudge<R> judge) {
    this(strict, judge, DELTA, DEFAULT_DELIMITER);
  }

  public Map<Entry,TraceComparisonResult> judgeOrCompare(Trace reference, Trace given) {
    return judgeOrCompare(reference, given, true);
  }

  public Map<Entry,TraceComparisonResult> judgeOrCompare(Trace reference, Trace given, boolean eagerTermination) {
    Map<Entry,TraceComparisonResult> results = new HashMap<>();
    //First, do all comparisons
    reference.getAll(Response.class).stream().filter(Judgement::evalWithComparison)
        .forEach(r -> results.put(r, compare(r, given.getResponse(r.requestId()))));
    reference.getAll(FunctionCall.class).stream().filter(Judgement::evalWithComparison)
        .forEach(r -> results.put(r, compare(r, given.getFunctionCall(r.requestId(), r.invocationId()))));
    if (eagerTermination && !results.isEmpty() && !results.values().stream().allMatch(TraceComparisonResult::isCorrect)) {
      return results;
    }
    reference.getAll(Response.class).stream().filter(Judgement::evalWithJudge)
        .forEach(r -> results.put(r, judge.judge(r, given.getResponse(r.requestId()))));
    reference.getAll(FunctionCall.class).stream().filter(Judgement::evalWithJudge)
        .forEach(r -> results.put(r, judge.judge(r, given.getFunctionCall(r.requestId(), r.invocationId()))));
    return results;
  }

  public TraceComparisonResult combinedJudgeOrCompare(Trace reference, Trace given) {
    return TraceComparisonResult.combine(judgeOrCompare(reference, given, true).values());
  }

  public TraceScore score(Trace reference, Trace given) {
    int count = 0, incorrect = 0, totalQuality = 0;
    for (Map.Entry<Entry, TraceComparisonResult> entry : judgeOrCompare(reference, given).entrySet()) {
      count++;
      if (!entry.getValue().isCorrect()) {
        incorrect++;
      }
      if (entry.getValue() instanceof QualitativeResult quality) {
        totalQuality += quality.getQualityScore();
      }
    }
    return new TraceScore(count, incorrect, totalQuality*1.0/count);
  }

  public TraceEquality compare(Response expected, Response given) {
    if (strict) {
      if (!expected.content().trim().equalsIgnoreCase(given.content().trim()))  {
        return notEqual("Content is not equal", expected.content(), given.content());
      } else {
        return TraceEquality.equal();
      }
    } else {
      return containsBagOfWords(given.content(), expected.content().split(DEFAULT_DELIMITER));
    }
  }

  public static TraceEquality containsBagOfWords(String message, String... words) {
    String messageLower = message.toLowerCase();
    List<String> nomatch = Arrays.stream(words).map(String::toLowerCase).filter(w -> !messageLower.contains(w))
        .collect(Collectors.toUnmodifiableList());
    if (!nomatch.isEmpty()) return TraceEquality.notEqual("Message does not contain: " + nomatch);
    else return TraceEquality.equal();
  }

  public TraceEquality compare(FunctionCall expected, FunctionCall given) {
    if (given == null || expected == null) return notEqual("Function call not found", expected, given);
    if (!expected.name().equalsIgnoreCase(given.name())) {
      return notEqual("Function names are different", expected.name(), given.name());
    } else if (expected.internal()!=given.internal()) {
      return notEqual("Function types are different", expected.internal(), given.internal());

    }
    return compare(expected.arguments(), given.arguments());
  }

  public TraceEquality compare(JsonNode expectedArguments, JsonNode givenArguments) {
    Iterator<String> fieldNames = expectedArguments.fieldNames();
    TraceEquality combined = TraceEquality.equal();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      if (!fieldEquals(expectedArguments.get(fieldName),givenArguments.get(fieldName))) {
        combined = combined.combine(notEqual("Argument field " + fieldName + " is different",
            expectedArguments.get(fieldName), givenArguments.get(fieldName)));
      }
    }
    if (strict && expectedArguments.size()!=givenArguments.size()) {
      combined = combined.combine(notEqual("Given arguments contains extra fields", expectedArguments, givenArguments));
    }
    return combined;
  }

  private TraceEquality notEqual(String message, Object expected, Object given) {
    return TraceEquality.notEqual(message + ": " + Objects.toString(expected) + " vs " + Objects.toString(given));
  }

  private boolean fieldEquals(JsonNode expected, JsonNode provided) {
    if (expected.isNumber()) {
      if (!provided.isNumber()) return false;
      return Math.abs(expected.asDouble() - provided.asDouble()) < delta;
    } else if (expected.isArray()) {
      if (!provided.isArray()) return false;
      if (provided.size()!=expected.size()) return false;
      for (int i = 0; i < expected.size(); i++) {
        if (!fieldEquals(expected.get(i), provided.get(i))) return false;
      }
      return true;
    } else {
      return expected.equals(provided);
    }
  }

  public static TraceEvaluator<TraceComparisonResult> equalityOnly(boolean strict) {
    return new TraceEvaluator<>(strict, new TraceJudge<TraceComparisonResult>() {

      @Override
      public TraceComparisonResult judge(Response reference, Response given) {
        throw new UnsupportedOperationException("Only support equality comparison.");
      }

      @Override
      public TraceComparisonResult judge(FunctionCall reference, FunctionCall given) {
        throw new UnsupportedOperationException("Only support equality comparison.");
      }
    });
  }

}
