package com.datasqrl.ai.api;

import com.google.auto.service.AutoService;
import java.util.Optional;
import org.apache.commons.configuration2.Configuration;

@AutoService(APIExecutorFactory.class)
public class GraphQLExecutorFactory implements APIExecutorFactory {

  public static final String TYPE = "graphql";
  public static final String AUTH_HEADERS_KEY = "auth";

  @Override
  public String getTypeName() {
    return TYPE;
  }

  @Override
  public APIExecutor create(Configuration configuration, String name) {
    BaseConfiguration baseConfiguration = APIExecutorFactory.readBaseConfiguration(configuration, name);
    Optional<String> authHeaders = Optional.ofNullable(configuration.getString(AUTH_HEADERS_KEY));
    return new GraphQLExecutor(baseConfiguration.url(), authHeaders);
  }
}
