package com.datasqrl.ai.models;

import com.datasqrl.ai.tool.ModelObservability;
import com.datasqrl.ai.tool.ToolManager;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.util.ErrorHandling;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;

public interface ChatProviderFactory {

  public static final String MODEL_PROVIDER_KEY = "provider";

  String getProviderName();

  ChatProvider create(Configuration modelConfiguration, ToolManager backend, String prompt, ModelObservability observability);

  static ChatProviderFactory fromConfiguration(Configuration modelConfiguration) {
    String provider = modelConfiguration.getString(MODEL_PROVIDER_KEY);
    ErrorHandling.checkArgument(provider!=null && !provider.isBlank(), "Need to configure `[%s]` in model configuration.", MODEL_PROVIDER_KEY);
    Optional<ChatProviderFactory> providerFact = ServiceLoader.load(ChatProviderFactory.class).stream()
        .map(Provider::get).filter(cpf -> cpf.getProviderName().equalsIgnoreCase(provider))
        .findFirst();
    ErrorHandling.checkArgument(providerFact.isPresent(), "Could not find model provider: " + provider);
    return providerFact.get();
  }

  static ChatProvider fromConfiguration(Map<String, Object> modelConfiguration, ToolManager backend, String prompt, ModelObservability observability) {
    MapConfiguration configuration = new MapConfiguration(modelConfiguration);
    return fromConfiguration(configuration).create(configuration,backend, prompt, observability);
  }

}
