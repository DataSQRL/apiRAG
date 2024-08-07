package com.datasqrl.ai.trace;

import java.util.ArrayList;
import java.util.List;

public interface TraceJudge<R extends TraceComparisonResult> {

  R judge(Trace.Response reference, Trace.Response given);

  R judge(Trace.FunctionCall reference, Trace.FunctionCall given);

}
