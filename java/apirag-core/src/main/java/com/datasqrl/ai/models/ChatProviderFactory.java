package com.datasqrl.ai.models;

import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.ModelObservability;
import org.apache.commons.configuration2.Configuration;

public interface ChatProviderFactory {

  String getProviderName();

  ChatClientProvider<?, ?> create(Configuration modelConfiguration, FunctionBackend backend,
      String prompt, ModelObservability observability);

}
