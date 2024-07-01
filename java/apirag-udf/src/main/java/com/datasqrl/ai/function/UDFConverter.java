package com.datasqrl.ai.function;

import com.datasqrl.ai.backend.FunctionDefinition;
import com.datasqrl.ai.backend.FunctionType;
import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import java.util.function.Function;
import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import lombok.Data;


public class UDFConverter {

  private static ObjectMapper mapper = new ObjectMapper();

  public static RuntimeFunctionDefinition getRuntimeFunctionDefinition(Class<? extends UserDefinedFunction> clazz) {
    FunctionDefinition funcDef = null;
    try {
      funcDef = mapper.treeToValue(getFunctionDefinition(clazz),  FunctionDefinition.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return RuntimeFunctionDefinition.builder()
        .type(FunctionType.local)
        .function(funcDef)
        .api(null)
        .context(List.of())
        .executable(getExecutableFunction(clazz))
        .build();
  }

  public static<T extends UserDefinedFunction> Function<JsonNode, Object> getExecutableFunction(Class<T> clazz) {
    return jsonNode -> {
      T parameters = null;
      try {
        parameters = mapper.treeToValue(jsonNode, clazz);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Could not parse function parameters", e);
      }
      return parameters.execute();
    };
  }



  public static JsonNode getFunctionDefinition(Class<? extends UserDefinedFunction> clazz) {
    ObjectNode rootNode = mapper.createObjectNode();
    rootNode.put("name", clazz.getSimpleName());
    getFunctionDescription(clazz).ifPresent(desc -> rootNode.put("description", desc));
    rootNode.set("parameters", generateParameters(clazz));
    return rootNode;
  }

  public static JsonNode generateParameters(Class<? extends UserDefinedFunction> clazz) {
    JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
    JsonNode jsonSchema = null;
    try {
      JsonSchema schema = schemaGen.generateSchema(clazz);
      jsonSchema = mapper.valueToTree(schema);
      ((ObjectNode) jsonSchema).remove("id");
      //Add list of "required" properties based on @Nonnull annotation on clazz
      List<String> requiredFields = Arrays.stream(clazz.getDeclaredFields())
          .filter(field -> field.isAnnotationPresent(Nonnull.class))
          .map(Field::getName).toList();
      if (!requiredFields.isEmpty()) {
        ArrayNode array = ((ObjectNode) jsonSchema).putArray("required");
        requiredFields.forEach(array::add);
      }
    } catch (JsonMappingException e) {
      throw new IllegalArgumentException("Could not generate schema for class " + clazz, e);
    }
    return jsonSchema;
  }

  public static Optional<String> getFunctionDescription(Class<?> clazz) {
    if (clazz.isAnnotationPresent(FunctionDescription.class)) {
      Annotation annotation = clazz.getAnnotation(FunctionDescription.class);
      FunctionDescription description = (FunctionDescription) annotation;
      return Optional.of(description.value());
    } else {
      return Optional.empty();
    }
  }

}
