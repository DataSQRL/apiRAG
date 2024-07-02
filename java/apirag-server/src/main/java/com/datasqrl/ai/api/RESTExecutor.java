package com.datasqrl.ai.api;

import com.datasqrl.ai.api.RestUtil.RestCall;
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
 * Implements the {@link APIExecutor} interface for REST APIs using Spring RestTemplate as the client.
 */
@Service
public class RESTExecutor implements APIExecutor {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final RestTemplate restTemplate;
  private final String endpoint;
  private final Optional<String> authHeader;

  public RESTExecutor(@NonNull String endpoint, @NonNull Optional<String> authHeader) {
    this.restTemplate = new RestTemplate();
    this.endpoint = endpoint;
    this.authHeader = authHeader;
  }

  @Override
  public void validate(APIQuery query) throws IllegalArgumentException {
    ErrorHandling.checkNotNullOrEmpty(query.getMethod(), "`method` cannot be empty");
    ErrorHandling.checkArgument(getMethod(query.getMethod())!=null);
    ErrorHandling.checkNotNullOrEmpty(query.getPath(), "`path` cannot be empty");
  }


  @Override
  public String executeQuery(APIQuery query, JsonNode arguments) throws IOException {
    RestCall call = RestUtil.createRestCall(query, arguments);
    HttpEntity<String> request = buildRequest(call.body());
    ResponseEntity<String> response = restTemplate.exchange(endpoint + call.path(), getMethod(query.getMethod()), request, String.class);

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

  private HttpEntity<String> buildRequest(JsonNode requestBody) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    authHeader.ifPresent(h -> headers.set("Authorization", h));
    return new HttpEntity<>(!requestBody.isEmpty()?objectMapper.writeValueAsString(requestBody):null, headers);
  }

  private static HttpMethod getMethod(String method) {
    return switch (method.trim().toUpperCase()) {
      case "GET" -> HttpMethod.GET;
      case "POST" -> HttpMethod.POST;
      case "PUT" -> HttpMethod.PUT;
      case "DELETE" -> HttpMethod.DELETE;
      default -> throw new IllegalArgumentException("Unsupported REST method: " + method);
    };
  }
}