package com.datasqrl.ai.trace;

import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.GenericFunctionCall;

import java.util.List;

public class TraceChatProvider implements ChatProvider {

  public TraceChatProvider(ChatProvider chatProvider, Trace.TraceBuilder traceBuilder) {
    this.chatProvider = chatProvider;
    this.traceBuilder = traceBuilder;
  }

  private final ChatProvider chatProvider;
  private final Trace.TraceBuilder traceBuilder;

  @Override
  public GenericChatMessage chat(String message, Context context) {
    TraceContext tContext = TraceContext.convert(context);
    traceBuilder.entry(new Trace.Message(tContext.getRequestId(), message));
    GenericChatMessage result = chatProvider.chat(message, context);
    if (result.getFunctionCall()!=null) {
      GenericFunctionCall fcall = result.getFunctionCall();
      traceBuilder.entry(new Trace.FunctionCall(tContext.getRequestId(), tContext.getInvocationId(),
          fcall.getName(), false, fcall.getArguments(), ""));
    } else {
      traceBuilder.entry(new Trace.Response(tContext.getRequestId(), result.getContent(), ""));
    }
    return result;
  }

  @Override
  public List<GenericChatMessage> getHistory(Context sessionContext, boolean includeFunctionCalls) {
    throw new UnsupportedOperationException("Should not retrieve history during trace generation");
  }
}
