package com.datasqrl.ai.backend;

import com.google.common.base.Stopwatch;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
public class MicrometerModelObservability implements ModelObservability {

  public static final String APP_NAME_TAG = "app";

  MeterRegistry meterRegistry;
  Timer latencyTimer;
  DistributionSummary inputTokenCounter;
  DistributionSummary outputTokenCounter;

  public MicrometerModelObservability(MeterRegistry meterRegistry, String applicationName) {
    this.meterRegistry = meterRegistry;
    this.latencyTimer = Timer.builder("model.execution.time")
        .tags(APP_NAME_TAG, applicationName)
        .publishPercentileHistogram()
        .register(meterRegistry);
    this.inputTokenCounter = DistributionSummary.builder("model.tokens.input")
        .tags(APP_NAME_TAG, applicationName)
        .register(meterRegistry);
    this.outputTokenCounter = DistributionSummary.builder("model.tokens.output")
        .tags(APP_NAME_TAG, applicationName)
        .register(meterRegistry);
  }

  @Override
  public Trace start() {
    return new StandardTrace();
  }

  @Value
  @AllArgsConstructor
  public class StandardTrace implements Trace {

    Stopwatch timer = Stopwatch.createStarted();

    @Override
    public void stop() {
      timer.stop();
    }

    @Override
    public void complete(int numInputTokens, int numOutputTokens) {
      latencyTimer.record(timer.elapsed());
      inputTokenCounter.record(numInputTokens);
      outputTokenCounter.record(numOutputTokens);
    }
  }
}
