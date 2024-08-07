package com.datasqrl.ai.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.datasqrl.ai.function.UDFConverter;
import com.datasqrl.ai.tool.FunctionType;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.trace.QualitativeTraceJudge.QualitativeResult;
import org.junit.jupiter.api.Test;

public class QualitativeTraceJudgeTest {

  @Test
  public void testFunction() {
    RuntimeFunctionDefinition function = UDFConverter.getRuntimeFunctionDefinition(
        QualitativeResult.class);
    assertEquals(FunctionType.client, function.getType());
    assertNull(function.getExecutable());
  }

}
