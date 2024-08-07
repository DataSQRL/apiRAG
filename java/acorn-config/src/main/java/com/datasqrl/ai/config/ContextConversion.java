package com.datasqrl.ai.config;

import com.datasqrl.ai.tool.Context;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;

public class ContextConversion {

  public static Context getContextFromUserId(String userId, List<String> contextKeys) {
    if (Strings.isNullOrEmpty(userId)) {
      Preconditions.checkArgument(contextKeys.isEmpty(), "Expected a user id for context: %s", contextKeys);
      return Context.of();
    }
    if (contextKeys.isEmpty()) return Context.of();
    Preconditions.checkArgument(contextKeys.size()==1, "Expected a single context key: %s", contextKeys);
    String key = contextKeys.get(0);
    try {
      Long numericValue = Long.parseLong(userId);
      return Context.of(Map.of(key, numericValue));
    } catch (NumberFormatException e) {
      //do nothing
    }
    return Context.of(Map.of(key, userId));
  }

}
