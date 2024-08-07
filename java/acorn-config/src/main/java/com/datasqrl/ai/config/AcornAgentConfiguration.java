package com.datasqrl.ai.config;

import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.APIExecutorFactory;
import com.datasqrl.ai.api.GraphQLSchemaConverter;
import com.datasqrl.ai.function.UDFConverter;
import com.datasqrl.ai.function.UserDefinedFunction;
import com.datasqrl.ai.function.builtin.BuiltinFunctions;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.datasqrl.ai.tool.ModelObservability;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.tool.ToolManager;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.ToolsBackendFactory;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.datasqrl.ai.util.ErrorHandling;
import com.datasqrl.ai.util.JsonUtil;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.JSONConfiguration;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Value
public class AcornAgentConfiguration {

  public static final String MODEL_PREFIX = "model";
  public static final String API_PREFIX = "apis";
  public static final String CONVERTER_PREFIX = "converter";

  public static final String PROMPT_KEY = "prompt";
  public static final String FUNCTIONS_KEY = "functions";
  public static final String CONTEXT_KEY = "context";

  Configuration baseConfiguration;
  Configuration modelConfiguration;
  List<RuntimeFunctionDefinition> toolFunctions;
  ModelObservability observability;


  private RuntimeFunctionDefinition loadFunction(String functionName) {
    String functionClassName = functionName;
    if (functionName.toLowerCase().endsWith(".json")) {
      //It's a URL that points to a json file with the client function definition
      try {
        URL url = ConfigurationUtil.getResourceFile(functionName);
        return UDFConverter.getClientFunction(url);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Could not read function definition from resource: " + functionName, e);
      }
    } else if (!functionName.contains(".")) {
      //Assume it's a builtin function
      functionClassName = BuiltinFunctions.PACKAGE_NAME + "." + functionName;
    }
    Class<?> functionClass = null;
    try {
      functionClass = Class.forName(functionClassName);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Could not locate user defined function: " +  functionClassName, e);
    }
    ErrorHandling.checkArgument(UserDefinedFunction.class.isAssignableFrom(functionClass), "Not a user defined function: %s", functionClassName);
    return UDFConverter.getRuntimeFunctionDefinition((Class<? extends UserDefinedFunction>)functionClass);
  }

  public ToolManager getToolManager() {
    Map<String,APIExecutor> apiExecutors = APIExecutorFactory.getAPIExecutors(baseConfiguration.subset(
        API_PREFIX));
    ErrorHandling.checkArgument(!apiExecutors.isEmpty(), "Need to configure at least one API in the configuration file under field `%s`",
        API_PREFIX);
    ToolsBackend backend = ToolsBackendFactory.of(toolFunctions, apiExecutors, Set.copyOf(getContext()));
    //Add functions
    baseConfiguration.getList(FUNCTIONS_KEY).stream().map(String.class::cast)
        .map(this::loadFunction).forEach(backend::addFunction);
    return backend;
  }

  public String getSystemPrompt() {
    String prompt = baseConfiguration.getString(PROMPT_KEY);
    ErrorHandling.checkArgument(prompt!=null, "Need to configure `[%s]` in configuration file.", PROMPT_KEY);
    ErrorHandling.checkArgument(!prompt.isBlank(), "`[%s]` cannot be empty.", PROMPT_KEY);
    return prompt;
  }

  public ChatProvider getChatProvider() {
    return getChatProvider(getToolManager());
  }

  public ChatProvider getChatProvider(ToolManager toolManager) {
    return ChatProviderFactory.fromConfiguration(modelConfiguration)
        .create(modelConfiguration, toolManager, getSystemPrompt(), observability);
  }

  public List<String> getContext() {
    if (baseConfiguration.containsKey(CONTEXT_KEY)) {
      return baseConfiguration.getList(String.class, CONTEXT_KEY);
    } else {
      return List.of();
    }
  }

  public static AcornAgentConfiguration fromFile(Path configPath, Path toolsPath) throws IOException {
    ErrorHandling.checkArgument(Files.isRegularFile(configPath), "Cannot access configuration file: %s", configPath);
    ErrorHandling.checkArgument(Files.isRegularFile(toolsPath), "Cannot access tools file: %s", toolsPath);
    JSONConfiguration baseConfig = JsonUtil.getConfiguration(configPath);
    String toolsContent;
    toolsContent = Files.readString(toolsPath);
    String extension = ConfigurationUtil.getFileExtension(toolsPath);
    List<RuntimeFunctionDefinition> tools;
    if (extension.equalsIgnoreCase("graphql") || extension.equalsIgnoreCase("graphqls")) {
      GraphQLSchemaConverter converter = new GraphQLSchemaConverter(
          baseConfig.subset(CONVERTER_PREFIX),
          APIExecutorFactory.DEFAULT_NAME);
      tools = converter.convert(toolsContent);
    } else {
      tools = ToolsBackendFactory.readTools(toolsContent);
    }
    return new AcornAgentConfiguration(baseConfig, baseConfig.subset("model"), tools, ModelObservability.NOOP);
  }

}
