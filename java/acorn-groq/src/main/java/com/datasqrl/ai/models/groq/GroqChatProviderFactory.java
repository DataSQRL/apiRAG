package com.datasqrl.ai.models.groq;

import com.datasqrl.ai.tool.ModelObservability;
import com.datasqrl.ai.tool.ToolManager;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.google.auto.service.AutoService;
import org.apache.commons.configuration2.Configuration;

@AutoService(ChatProviderFactory.class)
public class GroqChatProviderFactory implements ChatProviderFactory {

  public static final String PROVIDER_NAME = "groq";

  @Override
  public String getProviderName() {
    return PROVIDER_NAME;
  }

  @Override
  public ChatProvider create(Configuration modelConfiguration, ToolManager backend, String prompt, ModelObservability observability) {
    return new GroqChatProvider(new GroqModelConfiguration(modelConfiguration), backend, prompt, observability);
  }
}
