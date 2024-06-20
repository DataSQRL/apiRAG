package com.datasqrl.ai.backend;

import com.datasqrl.ai.backend.ModelObservability.Trace;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModelObservabilityTest {

  private MicrometerModelObservability modelObservability;
  private SimpleMeterRegistry meterRegistry;

  @BeforeEach
  public void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    modelObservability = new MicrometerModelObservability(meterRegistry, "testApp");
  }

  @Test
  public void testTrace() throws InterruptedException {
    // Start a trace
    for (int i = 1; i <= 10; i++) {
      Trace trace = modelObservability.start();
      // Simulate some work
      Thread.sleep(100);
      // Stop the trace
      trace.stop();
      // Complete the trace with some token counts
      trace.complete(10*i, 20*i);
    }

    // Check the metrics
    assertEquals(10, modelObservability.getLatencyTimer().count());
    assertEquals(550, modelObservability.getInputTokenCounter().totalAmount(), 0.001);
    assertEquals(1100, modelObservability.getOutputTokenCounter().totalAmount(), 0.001);
  }
}
