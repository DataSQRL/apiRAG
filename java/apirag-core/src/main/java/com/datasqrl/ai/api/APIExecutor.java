package com.datasqrl.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for querying and writing to an API
 */
public interface APIExecutor {

  void validate(APIQuery query) throws IllegalArgumentException;

  /**
   * Executes the given query with the provided arguments against the API and returns
   * the result as a String.
   *
   * @param query the query to execute
   * @param arguments the arguments for the query
   * @return The result of the query as a String
   * @throws IOException if the connection to the API failed or the query could not be executed
   */
  String executeQuery(APIQuery query, JsonNode arguments) throws IOException;

  /**
   * Executes an asynchronous request against the API for the given query with arguments.
   *
   * @param query the query to execute
   * @param arguments the arguments for the query
   * @return A future for the result
   */
  default CompletableFuture<String> executeQueryAsync(APIQuery query, JsonNode arguments) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return executeQuery(query, arguments);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

}
