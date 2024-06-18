package com.datasqrl.ai.comparison.config;

import com.datasqrl.ai.DataVisualizationFunction;
import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionDefinition;
import com.datasqrl.ai.backend.FunctionType;
import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.datasqrl.ai.models.ChatClientProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.datasqrl.ai.util.ErrorHandling;
import com.datasqrl.ai.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.JSONConfiguration;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.function.Function;

@Value
public class ComparisonConfiguration {

  public static final String MODEL_PREFIX = "model";
  public static final String MODEL_PROVIDER_KEY = "provider";

  public static final String API_URL_KEY = "api-url";
  public static final String PLOT_FCT_KEY = "plot-function";
  public static final String PROMPT_KEY = "prompt";

  public static final String AUTH_FIELD_KEY = "auth-field";
  public static final String AUTH_FIELD_INTEGER_KEY = "auth-is-integer";

  Configuration baseConfiguration;
  Configuration modelConfiguration;
  String toolsDefinition;

  public FunctionBackend getFunctionBackend() {
    String graphQLEndpoint = baseConfiguration.getString(API_URL_KEY);
    APIExecutor apiExecutor = new GraphQLExecutor(graphQLEndpoint);
    FunctionBackend backend;
    try {
      backend = FunctionBackend.of(toolsDefinition, apiExecutor);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not parse tools definition", e);
    }
    DataVisualizationFunction dataVisualizationFunction = getDataVizFunction();
    if (dataVisualizationFunction.isPresent()) {
      URL url = ComparisonConfiguration.class.getClassLoader().getResource(
          dataVisualizationFunction.getResourceFile().get());
      ErrorHandling.checkArgument(url!=null, "Invalid url: %s", url);
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        FunctionDefinition plotFunctionDef = objectMapper.readValue(url, FunctionDefinition.class);
        backend.addFunction(RuntimeFunctionDefinition.builder()
            .type(FunctionType.client)
            .function(plotFunctionDef)
            .context(List.of())
            .build());
      } catch (IOException e) {
        throw new IllegalArgumentException("Could not read plot function definition at: " + url, e);
      }
    }
    return backend;
  }

  public DataVisualizationFunction getDataVizFunction() {
    return ConfigurationUtil.getEnumFromString(
            DataVisualizationFunction.class,baseConfiguration.getString(PLOT_FCT_KEY, DataVisualizationFunction.none.name()))
        .orElseThrow(() -> new IllegalArgumentException("Not a valid configuration value for ["+PLOT_FCT_KEY+"]. Expected one of: " + Arrays.toString(
            DataVisualizationFunction.values())));
  }

  public String getSystemPrompt() {
    String prompt = baseConfiguration.getString(PROMPT_KEY);
    ErrorHandling.checkArgument(prompt!=null, "Need to configure `["+PROMPT_KEY+"]` in configuration file.");
    ErrorHandling.checkArgument(!prompt.isBlank(), "`["+PROMPT_KEY+"]` cannot be empty.");
    return prompt;
  }

  public ChatClientProvider getChatProvider() {
    FunctionBackend backend = getFunctionBackend();
    String systemPrompt = getSystemPrompt();
    return getChatProviderFactory().create(getModelConfiguration(), backend, systemPrompt);
  }

  public ChatProviderFactory getChatProviderFactory() {
    String provider = modelConfiguration.getString(MODEL_PROVIDER_KEY);
    ErrorHandling.checkArgument(provider!=null && !provider.isBlank(), "Need to configure `["+MODEL_PREFIX+"."+MODEL_PROVIDER_KEY+"]` in configuration file.");
    Optional<ChatProviderFactory> providerFact = ServiceLoader.load(ChatProviderFactory.class).stream()
        .map(Provider::get).filter(cpf -> cpf.getProviderName().equalsIgnoreCase(provider))
        .findFirst();
    ErrorHandling.checkArgument(providerFact.isPresent(), "Could not find model provider: " + provider);
    return providerFact.get();
  }

  public Function<String,Map<String,Object>> getContextFunction() {
    String fieldname = baseConfiguration.getString(AUTH_FIELD_KEY, "").trim();
    if (fieldname.isEmpty()) return s -> Map.of();
    boolean parseToInt = baseConfiguration.getBoolean(AUTH_FIELD_INTEGER_KEY, false);
    if (parseToInt) {
      return s -> Map.of(fieldname, Integer.parseInt(s));
    } else {
      return s -> Map.of(fieldname, s);
    }
  }

  public boolean hasAuth() {
    return baseConfiguration.containsKey(AUTH_FIELD_KEY);
  }

  public static ComparisonConfiguration fromFile(Path modelConfigPath, Path useCasePath, Path toolsPath) {
    ErrorHandling.checkArgument(Files.isRegularFile(modelConfigPath), "Cannot access configuration file: %s", modelConfigPath);
    ErrorHandling.checkArgument(Files.isRegularFile(toolsPath), "Cannot access tools file: %s", toolsPath);
    ErrorHandling.checkArgument(Files.isRegularFile(useCasePath), "Cannot access use case file: %s", useCasePath);
    JSONConfiguration baseConfig = JsonUtil.getConfiguration(useCasePath);
    JSONConfiguration modelConfig = JsonUtil.getConfiguration(modelConfigPath);
    String tools;
    try {
      tools = Files.readString(toolsPath);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read tools file: %s" + toolsPath, e);
    }
    return new ComparisonConfiguration(baseConfig, modelConfig, tools);
  }
}
