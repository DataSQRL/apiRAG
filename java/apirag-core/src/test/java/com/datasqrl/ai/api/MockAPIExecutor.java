package com.datasqrl.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.NonNull;
import lombok.Value;

@Value
public class MockAPIExecutor implements APIExecutor {

  Function<String, String> queryToResult;

  public static MockAPIExecutor of(@NonNull String uniformResult) {
    return new MockAPIExecutor(s -> uniformResult);
  }

  @Override
  public void validate(APIQuery query) throws IllegalArgumentException {
    return;
  }

  @Override
  public String executeQuery(APIQuery query, JsonNode arguments) throws IOException {
    return queryToResult.apply(query.getQuery());
  }

  @Override
  public CompletableFuture<String> executeQueryAsync(APIQuery query, JsonNode arguments) {
    return CompletableFuture.completedFuture("mock write");
  }
}
