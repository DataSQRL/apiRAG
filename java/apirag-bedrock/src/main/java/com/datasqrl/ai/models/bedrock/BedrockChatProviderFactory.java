package com.datasqrl.ai.models.bedrock;

import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.ModelObservability;
import com.datasqrl.ai.models.AbstractChatProviderFactory;
import com.datasqrl.ai.models.ChatClientProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.google.auto.service.AutoService;
import org.apache.commons.configuration2.Configuration;

@AutoService(ChatProviderFactory.class)
public class BedrockChatProviderFactory extends AbstractChatProviderFactory {

  public static final String PROVIDER_NAME = "bedrock";

  @Override
  public String getProviderName() {
    return PROVIDER_NAME;
  }

  @Override
  public ChatClientProvider<?, ?> create(Configuration modelConfiguration, FunctionBackend backend, String prompt, ModelObservability observability) {
    return new BedrockChatProvider(getModel(modelConfiguration, BedrockChatModel.class), backend, prompt, observability);
  }
}
