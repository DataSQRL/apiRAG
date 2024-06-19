package com.datasqrl.ai.models.groq;

import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.ModelObservability;
import com.datasqrl.ai.models.AbstractChatProviderFactory;
import com.datasqrl.ai.models.ChatClientProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.google.auto.service.AutoService;
import org.apache.commons.configuration2.Configuration;

@AutoService(ChatProviderFactory.class)
public class GroqChatProviderFactory extends AbstractChatProviderFactory {

  public static final String PROVIDER_NAME = "groq";

  @Override
  public String getProviderName() {
    return PROVIDER_NAME;
  }

  @Override
  public ChatClientProvider<?, ?> create(Configuration modelConfiguration, FunctionBackend backend, String prompt, ModelObservability observability) {
    return new GroqChatProvider(getModel(modelConfiguration, GroqChatModel.class), backend, prompt, observability);
  }
}
