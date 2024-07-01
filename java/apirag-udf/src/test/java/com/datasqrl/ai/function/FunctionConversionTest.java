package com.datasqrl.ai.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.datasqrl.ai.backend.FunctionType;
import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.datasqrl.ai.function.builtin.BuiltinFunctions;
import com.datasqrl.ai.function.builtin.CurrentTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

public class FunctionConversionTest {

  public static final ObjectMapper mapper = new ObjectMapper();

  @Test
  @SneakyThrows
  public void testUDFSchema() {
    RuntimeFunctionDefinition function = UDFConverter.getRuntimeFunctionDefinition(TestAddFunction.class);
    assertEquals("TestAddFunction", function.getName());
    //Create a Json node with two fields: arg1 and arg2
    JsonNode jsonNode = mapper.createObjectNode().put("arg1",2).put("arg2", 3);
    assertEquals(5.0, (double)function.getExecutable().apply(jsonNode), 0.00001);
    System.out.println(function);
  }

  @Test
  public void testCurrentTime() {
    RuntimeFunctionDefinition function = UDFConverter.getRuntimeFunctionDefinition(CurrentTime.class);
    String time = function.getExecutable().apply(mapper.createObjectNode()).toString();
    assertTrue(Instant.parse(time).isBefore(Instant.now()));
    assertTrue(Instant.parse(time).isAfter(Instant.now().minus(200, java.time.temporal.ChronoUnit.SECONDS)));
  }

  @Test
  public void listBuiltinFunctions() {
    Set<Class<? extends UserDefinedFunction>> builtinFunctions = getBuiltinFunctions();
    for (Class<? extends UserDefinedFunction> udf : builtinFunctions) {
      assertFalse(udf.isInterface());
      //Make sure udf has the FunctionDescription Annotation
      assertNotNull(udf.getAnnotation(FunctionDescription.class));
      RuntimeFunctionDefinition function = UDFConverter.getRuntimeFunctionDefinition(udf);
      System.out.println(function);
      assertEquals(udf.getSimpleName(), function.getName());
      assertNotNull(function.getExecutable());
      assertEquals(FunctionType.local, function.getType());

    }
  }

  @SneakyThrows
  public static Set<Class<? extends UserDefinedFunction>> getBuiltinFunctions() {
    Reflections reflections = new Reflections(BuiltinFunctions.PACKAGE_NAME);
    return reflections.getSubTypesOf(UserDefinedFunction.class);
  }

}
