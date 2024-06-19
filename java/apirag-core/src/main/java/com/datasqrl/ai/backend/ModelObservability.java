package com.datasqrl.ai.backend;

import java.util.Map;

public interface ModelObservability {

  Trace start();

  public interface Trace {

    void stop(int numInputTokens, int numOutputTokens);

  }

  public static final ModelObservability NOOP = new ModelObservability() {
    @Override
    public Trace start() {
      return new Trace() {
        @Override
        public void stop(int numInputTokens, int numOutputTokens) {

        }
      };
    }
  };

}
