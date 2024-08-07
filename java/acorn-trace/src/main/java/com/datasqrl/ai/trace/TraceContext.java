package com.datasqrl.ai.trace;

import com.datasqrl.ai.tool.Context;
import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TraceContext implements Context {

  private int requestId;
  private int invocationId;
  private final Map<String, Object> secure;

  public static TraceContext of() {
    return of(Collections.emptyMap());
  }

  public static TraceContext of(Map<String, Object> secure) {
    return new TraceContext(0, 0, secure);
  }

  public static TraceContext convert(Context context) {
    Preconditions.checkArgument(context instanceof TraceContext, "Expected a TraceContext");
    return (TraceContext) context;
  }

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

  @Override
  public Map<String, Object> asMap() {
    Map<String, Object> result = new HashMap<>(secure.size() + 2);
    forEach(result::put);
    return result;
  }

  @Override
  public void nextInvocation() {
    this.invocationId++;
  }

  public TraceContext nextRequest() {
    return new TraceContext(++requestId, 0, secure);
  }

  public void setRequestIndex(int requestId) {
    this.requestId = requestId;
  }
}
