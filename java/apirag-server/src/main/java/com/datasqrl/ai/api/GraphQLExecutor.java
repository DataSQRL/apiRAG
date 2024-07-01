package com.datasqrl.ai.api;

import com.datasqrl.ai.util.ErrorHandling;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Implements the {@link APIExecutor} interface for GraphQL APIs using Spring RestTemplate as the client.
 */
@Service
public class GraphQLExecutor implements APIExecutor {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final RestTemplate restTemplate;
  private final String endpoint;
  private final Optional<String> authHeader;

  public GraphQLExecutor(@NonNull String endpoint, @NonNull Optional<String> authHeader) {
    this.restTemplate = new RestTemplate();
    this.endpoint = endpoint;
    this.authHeader = authHeader;
  }

  @Override
  public void validate(APIQuery query) throws IllegalArgumentException {
    ErrorHandling.checkNotNullOrEmpty(query.getQuery(), "`query` cannot be empty");
  }

  @Override
  public String executeQuery(APIQuery query, JsonNode arguments) throws IOException {
    HttpEntity<String> request = buildRequest(query.getQuery(), arguments);
    ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);

    if (!response.getStatusCode().is2xxSuccessful()) {
      throw new IOException("Query failed: " + response);
    }

    return response.getBody();
  }

  @Override
  public CompletableFuture<String> executeQueryAsync(APIQuery query, JsonNode arguments) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return executeQuery(query, arguments);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private HttpEntity<String> buildRequest(String query, JsonNode arguments) throws IOException {
    JsonNode requestBody = objectMapper.createObjectNode()
        .put("query", query)
        .set("variables", arguments);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    authHeader.ifPresent(h -> headers.set("Authorization", h));

    return new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
  }
}