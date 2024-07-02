package com.datasqrl.ai.backend;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Definition of a chat function that can be invoked by the language model.
 *
 * This is essentially a java definition of the json structure OpenAI uses to represent
 * functions which is used by most LLMs.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FunctionDefinition {

  private String name;

  private String description;

  private Parameters parameters;


  public FunctionDefinition removeContext(Set<String> context) {
    Predicate<String> fieldFilter = getFieldFilter(context);
    Parameters newParams = Parameters.builder()
        .type(parameters.getType())
        .required(parameters.getRequired().stream()
            .filter(fieldFilter).collect(
                Collectors.toUnmodifiableList()))
        .properties(parameters.getProperties().entrySet().stream()
            .filter(e -> fieldFilter.test(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
        .build();

    return FunctionDefinition.builder()
        .name(getName())
        .description(getDescription())
        .parameters(newParams)
        .build();
  }

  public static Predicate<String> getFieldFilter(Set<String> fieldList) {
    Set<String> contextFilter = fieldList.stream().map(String::toLowerCase).collect(Collectors.toUnmodifiableSet());
    return field -> !contextFilter.contains(field.toLowerCase());
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Parameters {

    private String type;
    private Map<String, Argument> properties = Map.of();
    private List<String> required = List.of();

  }

  @Data
  public static class Argument {

    private String type;
    private String description;
    private Argument items;
    @JsonProperty("enum")
    private Set<?> enumValues;
  }

}
