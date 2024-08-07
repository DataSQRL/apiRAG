package com.datasqrl.ai.tool;

public interface ModelObservability {

  ModelInvocation start();

  interface ModelInvocation {

    void stop(int numInputTokens, int numOutputTokens);

    void fail(Exception e);

  }

  public static final ModelObservability NOOP = new ModelObservability() {
    @Override
    public ModelInvocation start() {
      return new ModelInvocation() {
        @Override
        public void stop(int numInputTokens, int numOutputTokens) {

        }

        @Override
        public void fail(Exception e) {

        }
      };
    }
  };

}
