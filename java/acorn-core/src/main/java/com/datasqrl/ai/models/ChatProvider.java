package com.datasqrl.ai.models;

import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.GenericChatMessage;
import java.util.List;

public interface ChatProvider {

  GenericChatMessage chat(String message, Context context);

  List<GenericChatMessage> getHistory(Context sessionContext, boolean includeFunctionCalls);

}
