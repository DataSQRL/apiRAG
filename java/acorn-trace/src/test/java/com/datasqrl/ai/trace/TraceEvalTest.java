package com.datasqrl.ai.trace;

import com.datasqrl.ai.trace.QualitativeTraceJudge.QualitativeResult;
import com.datasqrl.ai.trace.Trace.Entry;
import com.datasqrl.ai.trace.Trace.FunctionCall;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.datasqrl.ai.trace.TraceAssertions.assertContainsIgnoreCase;
import static com.datasqrl.ai.trace.TraceAssertions.assertSameFunctionCalls;

public class TraceEvalTest {

  public static final Map<String,Object> JUDGE_MODEL_CONFIG = Map.of(
      "provider", "openai",
      "name", "gpt-4o-mini",
      "temperature", 0.2,
      "max_output_tokens", 512
  );

  @Test
  @SneakyThrows
  public void evaluateTrace() {
    Trace finance = Trace.loadFromURL(TraceEvalTest.class.getResource("/traces/trace-finance.json"));
    Trace reference = Trace.loadFromURL(TraceEvalTest.class.getResource(
        "/traces/trace-finance-comparison.json"));
    Assertions.assertEquals(18, finance.size());
    assertContainsIgnoreCase(finance.getResponse(0).content(), "transaction", "spending");
    assertSameFunctionCalls(reference.getAll(FunctionCall.class), finance, false);
    FunctionCall viz1 = finance.getFunctionCall(1,1);
    Assertions.assertEquals(Set.of(2601.59, 2730.05, 2328.52, 2344.55, 2218.76), Set.copyOf(ArgumentHelper.asDoubleList(viz1.arguments().get("values"))));
    Assertions.assertTrue(List.of("bar","column").contains(viz1.arguments().get("chartType").asText()));
  }

  @Test
  @Disabled("requires openai api key")
  @SneakyThrows
  public void judgeTrace() {
    Trace finance = Trace.loadFromURL(TraceEvalTest.class.getResource("/traces/trace-finance.json"));
    Trace reference = Trace.loadFromURL(TraceEvalTest.class.getResource("/traces/trace-finance-judgeAndCompare.json"));
    Assertions.assertEquals(18, finance.size());

    QualitativeTraceJudge judge = QualitativeTraceJudge.fromConfiguration(new MapConfiguration(JUDGE_MODEL_CONFIG));
    TraceEvaluator<QualitativeResult> evaluator = new TraceEvaluator<>(false, judge);
    Map<Entry,TraceComparisonResult> results = evaluator.judgeOrCompare(reference, finance);
    results.forEach((k,v) -> {
      System.out.println(k);
      System.out.println(v);
    });
    TraceComparisonResult.combine(results.values()).assertCorrect();
  }

}
