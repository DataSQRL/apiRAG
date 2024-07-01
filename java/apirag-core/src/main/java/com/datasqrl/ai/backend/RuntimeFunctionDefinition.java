package com.datasqrl.ai.backend;

import com.datasqrl.ai.api.APIQuery;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data class that defines a function with additional metadata for how
 * the function gets executed against the API and what context it requires.
 *
 * @see FunctionDefinition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeFunctionDefinition {

  private FunctionType type;
  private FunctionDefinition function;
  private List<String> context;
  private APIQuery api;
  @JsonIgnore
  private Function<JsonNode, Object> executable;
  @JsonIgnore
  private Integer numTokens;

  @JsonIgnore
  public String getName() {
    return function.getName();
  }

  public int getNumTokens(ModelAnalyzer analyzer) {
    if (numTokens==null) {
      numTokens = analyzer.countTokens(getChatFunction());
    }
    return numTokens;
  }


  /**
   *
   * @return A {@link FunctionDefinition} for this function that can be invoked by a language model.
   */
  @JsonIgnore
  public FunctionDefinition getChatFunction() {
    return function.removeContext(context!=null?Set.copyOf(context):Set.of());
  }


}
