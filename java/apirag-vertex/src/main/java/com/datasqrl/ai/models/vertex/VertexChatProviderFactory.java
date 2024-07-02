package com.datasqrl.ai.models.vertex;

import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.models.AbstractChatProviderFactory;
import com.datasqrl.ai.models.ChatClientProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.datasqrl.ai.util.ErrorHandling;
import com.google.auto.service.AutoService;
import org.apache.commons.configuration2.Configuration;

@AutoService(ChatProviderFactory.class)
public class VertexChatProviderFactory extends AbstractChatProviderFactory {

  public static final String PROVIDER_NAME = "vertex";
  private static final String VERTEX_PROJECT_ID_KEY = "vertex-project-id";
  private static final String VERTEX_LOCATION_KEY = "vertex-location";

  @Override
  public String getProviderName() {
    return PROVIDER_NAME;
  }

  @Override
  public ChatClientProvider<?, ?> create(Configuration modelConfiguration, FunctionBackend backend, String prompt) {
    String vertexProjectId = modelConfiguration.getString(VERTEX_PROJECT_ID_KEY);
    ErrorHandling.checkArgument(vertexProjectId != null && !vertexProjectId.isBlank(), "Need to configure vertex-project-id.");
    String vertexLocation = modelConfiguration.getString(VERTEX_LOCATION_KEY);
    ErrorHandling.checkArgument(vertexLocation != null && !vertexLocation.isBlank(), "Need to configure vertex-location.");
    return new VertexChatProvider(getModel(modelConfiguration, VertexChatModel.class), vertexProjectId, vertexLocation, backend, prompt);
  }
}
