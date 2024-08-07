package com.datasqrl.ai.function;

import com.datasqrl.ai.tool.GenericFunctionCall;
import com.datasqrl.ai.tool.FunctionDefinition;
import com.datasqrl.ai.tool.FunctionType;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.util.ErrorHandling;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;


public class UDFConverter {

  private static final ObjectMapper mapper = new ObjectMapper();


  public static RuntimeFunctionDefinition getClientFunction(URL functionDefinitionURL) {
    ErrorHandling.checkArgument(functionDefinitionURL!=null, "Invalid url: %s", functionDefinitionURL);
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      FunctionDefinition plotFunctionDef = objectMapper.readValue(functionDefinitionURL, FunctionDefinition.class);
      return getClientFunction(plotFunctionDef);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read client function definition at: " + functionDefinitionURL, e);
    }
  }

  public static RuntimeFunctionDefinition getClientFunction(FunctionDefinition clientFunction) {
    return RuntimeFunctionDefinition.builder()
        .type(FunctionType.client)
        .function(clientFunction)
        .context(List.of())
        .build();
  }

  public static RuntimeFunctionDefinition getRuntimeFunctionDefinition(Class<? extends UserDefinedFunction> clazz) {
    FunctionDefinition funcDef;
    try {
      funcDef = mapper.treeToValue(getFunctionDefinition(clazz),  FunctionDefinition.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    boolean isLocal = !isClientFunction(clazz);
    return RuntimeFunctionDefinition.builder()
        .type(isLocal?FunctionType.local:FunctionType.client)
        .function(funcDef)
        .api(null)
        .context(List.of())
        .executable(isLocal?getExecutableFunction(clazz):null)
        .build();
  }

  private static boolean isClientFunction(Class<? extends UserDefinedFunction> clazz) {
    try {
      Method method = clazz.getMethod("isClientFunction");
      if (Modifier.isStatic(method.getModifiers())) {
        return ((boolean) method.invoke(null));
      }
    } catch (NoSuchMethodException e) {
      //ignore
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  public static<U extends UserDefinedFunction> U getFunctionCall(GenericFunctionCall functionCall, Class<U> clazz) {
    ErrorHandling.checkArgument(functionCall.getName().equalsIgnoreCase(clazz.getSimpleName()), "Not the same functions: %s vs %s", functionCall.getName(), clazz.getSimpleName());
    return getFunctionCall(functionCall.getArguments(), clazz);
  }

  public static<U extends UserDefinedFunction> U getFunctionCall(JsonNode arguments, Class<U> clazz) {
    try {
      return mapper.treeToValue(arguments, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Could not parse function parameters", e);
    }
  }

  public static<U extends UserDefinedFunction> Function<JsonNode, Object> getExecutableFunction(Class<U> clazz) {
    return jsonNode -> {
      U udfCall = getFunctionCall(jsonNode, clazz);
      return udfCall.execute();
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
      FunctionDescription description = clazz.getAnnotation(FunctionDescription.class);
      return Optional.of(description.value());
    } else {
      return Optional.empty();
    }
  }

}
