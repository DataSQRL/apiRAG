package com.datasqrl.ai.tool;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

/**
 * The {@link ContextImpl} class captures the context of an agent interaction.
 * It has a request id that is unique for each interaction and an invocation counter
 * for the number of times the LLM is invoked in the course of producing a request response.
 *
 * Additional key-value pairs can be provided to securely pass information to the function
 * calls outside the LLM call stack.
 *
 * The request id and secure information are static for the duration of an interaction.
 * The counter is incremented for each time the LLM is invoked.
 */
@AllArgsConstructor
@Getter
public class ContextImpl implements Context {

  private final String requestId;
  private int invocationId;
  private final Map<String, Object> secure;

  public Object get(String key) {
    if (key.equalsIgnoreCase(REQUEST_ID_KEY)) return requestId;
    if (key.equalsIgnoreCase(INVOCATION_KEY)) return invocationId;
    return secure.get(key);
  }

  public void forEach(BiConsumer<String, Object> action) {
    Objects.requireNonNull(action);
    action.accept(REQUEST_ID_KEY, requestId);
    action.accept(INVOCATION_KEY, invocationId);
    secure.forEach(action);
  }

  public Map<String,Object> asMap() {
    Map<String, Object> result = new HashMap<>(secure.size()+2);
    forEach(result::put);
    return result;
  }

  public void nextInvocation() {
    this.invocationId++;
  }
}
