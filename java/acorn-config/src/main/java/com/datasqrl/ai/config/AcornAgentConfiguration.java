package com.datasqrl.ai.config;

import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.APIExecutorFactory;
import com.datasqrl.ai.api.GraphQLSchemaConverter;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.ToolsBackendFactory;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.function.UDFConverter;
import com.datasqrl.ai.function.UserDefinedFunction;
import com.datasqrl.ai.function.builtin.BuiltinFunctions;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.datasqrl.ai.util.ErrorHandling;
import com.datasqrl.ai.util.JsonUtil;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.JSONConfiguration;

@Value
public class AcornAgentConfiguration {

  public static final String MODEL_PREFIX = "model";
  public static final String API_PREFIX = "apis";
  public static final String CONVERTER_PREFIX = "converter";

  public static final String CLIENT_FUNCTIONS_KEY = "client_functions";
  public static final String PROMPT_KEY = "prompt";
  public static final String LOCAL_FUNCTIONS_KEY = "local_functions";
  public static final String CONTEXT_KEY = "context";

  Configuration baseConfiguration;
  Configuration modelConfiguration;
  List<RuntimeFunctionDefinition> toolFunctions;

  private RuntimeFunctionDefinition loadLocalFunction(String functionClassName) {
    if (!functionClassName.contains(".")) {
      //Assume it's a builtin function
      functionClassName = BuiltinFunctions.PACKAGE_NAME + "." + functionClassName;
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

  public ToolsBackend getFunctionBackend() {
    Map<String,APIExecutor> apiExecutors = APIExecutorFactory.getAPIExecutors(baseConfiguration.subset(
        API_PREFIX));
    ErrorHandling.checkArgument(!apiExecutors.isEmpty(), "Need to configure at least one API in the configuration file under field `%s`",
        API_PREFIX);
    ToolsBackend backend = ToolsBackendFactory.of(toolFunctions, apiExecutors, Set.copyOf(getContext()));
    //Add client functions
    baseConfiguration.getList(CLIENT_FUNCTIONS_KEY).stream().map(String.class::cast)
        .forEach(fctName -> {
          ClientSideFunctions clientFct = ClientSideFunctions.forName(fctName).orElseThrow(() ->
              new IllegalArgumentException(String.format("Not a valid client function: %s. Should be one of: %s", fctName, Arrays.toString(ClientSideFunctions.values()))));
          URL url = ConfigurationUtil.getResourceFile(clientFct.getResourceFile());
          UDFConverter.addClientFunction(backend, url);
        });
    //Add local functions
    baseConfiguration.getList(LOCAL_FUNCTIONS_KEY).stream().map(String.class::cast)
        .map(this::loadLocalFunction).forEach(backend::addFunction);
    return backend;
  }

  public String getSystemPrompt() {
    String prompt = baseConfiguration.getString(PROMPT_KEY);
    ErrorHandling.checkArgument(prompt!=null, "Need to configure `[%s]` in configuration file.", PROMPT_KEY);
    ErrorHandling.checkArgument(!prompt.isBlank(), "`[%s]` cannot be empty.", PROMPT_KEY);
    return prompt;
  }

  public ChatProvider getChatProvider() {
    ToolsBackend backend = getFunctionBackend();
    String systemPrompt = getSystemPrompt();
    return ChatProviderFactory.fromConfiguration(modelConfiguration).create(getModelConfiguration(), backend, systemPrompt);
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
    return new AcornAgentConfiguration(baseConfig, baseConfig.subset("model"), tools);
  }

}
