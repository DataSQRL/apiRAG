package com.datasqrl.ai.comparison;

import com.datasqrl.ai.tool.ModelObservability;
import com.datasqrl.ai.tool.ToolObservability;
import com.google.common.base.Stopwatch;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@Value
public class MicrometerObservability implements ModelObservability, ToolObservability {

  public static final String APP_NAME_TAG = "app";

  MeterRegistry meterRegistry;
  Timer modelLatencyTimer;
  DistributionSummary inputTokenCounter;
  DistributionSummary outputTokenCounter;
  Counter failedModelCounter;
  Timer toolLatencyTimer;
  Counter failedToolCounter;
  Counter toolInvalidCounter;

  public MicrometerObservability(MeterRegistry meterRegistry, String applicationName) {
    this.meterRegistry = meterRegistry;
    this.modelLatencyTimer = Timer.builder("model.execution.time")
        .tags(APP_NAME_TAG, applicationName)
        .publishPercentileHistogram()
        .publishPercentiles(0.1, 0.5, 0.9)
        .register(meterRegistry);
    this.inputTokenCounter = DistributionSummary.builder("model.tokens.input")
        .tags(APP_NAME_TAG, applicationName)
        .register(meterRegistry);
    this.outputTokenCounter = DistributionSummary.builder("model.tokens.output")
        .tags(APP_NAME_TAG, applicationName)
        .register(meterRegistry);
    this.failedModelCounter = Counter.builder("model.execution.failed")
        .tags(APP_NAME_TAG, applicationName)
        .register(meterRegistry);
    this.toolInvalidCounter = Counter.builder("tool.call.invalid")
        .tags(APP_NAME_TAG, applicationName)
        .register(meterRegistry);
    this.failedToolCounter = Counter.builder("tool.execution.failed")
        .tags(APP_NAME_TAG, applicationName)
        .register(meterRegistry);
    this.toolLatencyTimer = Timer.builder("tool.execution.time")
        .tags(APP_NAME_TAG, applicationName)
        .publishPercentileHistogram()
        .publishPercentiles(0.1, 0.5, 0.9)
        .register(meterRegistry);
  }

  @Override
  public ModelInvocation start() {
    return new StandardModelInvocation();
  }

  public String exportToCSV() {
    StringBuilder s = new StringBuilder();
    s.append(modelLatencyTimer.count()).append(", ");
    s.append(modelLatencyTimer.totalTime(TimeUnit.MILLISECONDS)).append(", ");
    s.append(modelLatencyTimer.mean(TimeUnit.MILLISECONDS)).append(", ");
    s.append(modelLatencyTimer.max(TimeUnit.MILLISECONDS)).append(", ");
    s.append(inputTokenCounter.totalAmount()).append(", ");
    s.append(inputTokenCounter.mean()).append(", ");
    s.append(outputTokenCounter.totalAmount()).append(", ");
    s.append(outputTokenCounter.mean()).append(", ");
    s.append(toolInvalidCounter.count());
    return s.toString();
  }


  @Value
  @AllArgsConstructor
  public class StandardModelInvocation implements ModelInvocation {

    Stopwatch timer = Stopwatch.createStarted();

    @Override
    public void stop(int numInputTokens, int numOutputTokens) {
      modelLatencyTimer.record(timer.elapsed());
      inputTokenCounter.record(numInputTokens);
      outputTokenCounter.record(numOutputTokens);
    }

    @Override
    public void fail(Exception e) {
      failedModelCounter.increment();
    }
  }


  @Override
  public ToolCall start(String toolName) {
    return new StandardToolCall();
  }

  public class StandardToolCall implements ToolCall {

    Stopwatch timer = Stopwatch.createStarted();

    @Override
    public void stop() {
      toolLatencyTimer.record(timer.elapsed());
    }

    @Override
    public void fail(Exception e) {
      failedToolCounter.increment();
    }
  }
}
