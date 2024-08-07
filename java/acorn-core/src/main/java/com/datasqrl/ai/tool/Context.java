package com.datasqrl.ai.tool;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * The {@link Context} class captures the context of an agent interaction.
 * It has a request id that is unique for each interaction and an invocation counter
 * for the number of times the LLM is invoked in the course of producing a request response.
 *
 * Additional key-value pairs can be provided to securely pass information to the function
 * calls outside the LLM call stack.
 *
 * The request id and secure information are static for the duration of an interaction.
 * The counter is incremented for each time the LLM is invoked.
 */
public interface Context {

  String REQUEST_ID_KEY = "requestid";
  String INVOCATION_KEY = "invocationid";

  default Object get(String key) {
    return asMap().get(key);
  }

  default void forEach(BiConsumer<String, Object> action) {
    asMap().forEach(action);
  }

  Map<String,Object> asMap();

  void nextInvocation();

  static Context of() {
    return of(Collections.emptyMap());
  }

  static Context of(Map<String, Object> secure) {
    //Invocations are incremented before a model is called, hence we start with -1 (to indicate
    //model has not yet been called) so the first invocation is 0.
    return new ContextImpl(UUID.randomUUID().toString(), -1, secure);
  }
}
