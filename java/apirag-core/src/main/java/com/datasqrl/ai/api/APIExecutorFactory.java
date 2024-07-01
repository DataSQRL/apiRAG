package com.datasqrl.ai.api;


import com.datasqrl.ai.util.ConfigurationUtil;
import com.datasqrl.ai.util.ErrorHandling;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.tuple.Pair;

public interface APIExecutorFactory {

  public static final String DEFAULT_NAME = "default";
  public static final String TYPE_KEY = "type";
  public static final String URL_KEY = "url";


  String getTypeName();

  APIExecutor create(Configuration configuration, String name);


  record BaseConfiguration(String name, String type, String url) {
  }

  public static BaseConfiguration readBaseConfiguration(Configuration configuration, String name) {
    String type = configuration.getString(TYPE_KEY);
    ErrorHandling.checkArgument(type!=null && !type.isBlank(), "Need to configure `%s` for api `%s` in configuration file.", TYPE_KEY, name);
    String url = configuration.getString(URL_KEY);
    ErrorHandling.checkArgument(url!=null && !url.isBlank(), "Need to configure `%s` for api `%s` in configuration file.", URL_KEY, name);
    return new BaseConfiguration(name, type, url);
  }

  public static Map<String, APIExecutor> getAPIExecutors(Configuration configuration) {
    Map<String, APIExecutor> apiExecutors = new HashMap<>();
    if (configuration.containsKey(TYPE_KEY) && configuration.containsKey(URL_KEY)) {
      //Read single API configuration
      APIExecutor apiSpec = instantiateAPI(configuration, DEFAULT_NAME);
      apiExecutors.put(DEFAULT_NAME.trim().toLowerCase(), apiSpec);
    } else {
      //Read multiple API configurations
      for (String key : ConfigurationUtil.getSubKeys(configuration)) {
        Configuration apiConfig = configuration.subset(key);
        APIExecutor apiSpec = instantiateAPI(apiConfig, key);
        apiExecutors.put(key.trim().toLowerCase(), apiSpec);
      }
    }
    return apiExecutors;
  }

  private static APIExecutor instantiateAPI(Configuration apiConfig, String name) {
    BaseConfiguration baseAPIConfig = readBaseConfiguration(apiConfig, name);
    Optional<APIExecutorFactory> providerFact = ServiceLoader.load(APIExecutorFactory.class).stream()
        .map(Provider::get).filter(cpf -> cpf.getTypeName().equalsIgnoreCase(baseAPIConfig.type()))
        .findFirst();
    ErrorHandling.checkArgument(providerFact.isPresent(), "Could not find API executor for API `%s`: %s", APIExecutorFactory.TYPE_KEY, baseAPIConfig.type());
    return providerFact.get().create(apiConfig, name);
  }

}
