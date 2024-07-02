package com.datasqrl.ai.models.groq;

import com.datasqrl.ai.backend.ChatSession;
import com.datasqrl.ai.backend.ContextWindow;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.GenericChatMessage;
import com.datasqrl.ai.models.ChatClientProvider;
import com.datasqrl.ai.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.theokanning.openai.service.OpenAiService.defaultClient;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

@Slf4j
public class GroqChatProvider extends ChatClientProvider<ChatMessage, ChatFunctionCall> {

  private final GroqChatModel model;
  private final OpenAiService service;
  private final String systemPrompt;
  private ChatFunctionCall errorFunctionCall = null;
  public static final String GROQ_URL = "https://api.groq.com/openai/v1/";

  public GroqChatProvider(GroqChatModel model, FunctionBackend backend, String systemPrompt) {
    super(backend, new GroqModelBindings(model));
    this.model = model;
    this.systemPrompt = systemPrompt;
    String groqApiKey = System.getenv("GROQ_API_KEY");
    ObjectMapper mapper = defaultObjectMapper();
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.setLevel(HttpLoggingInterceptor.Level.NONE); // Change to .BODY to see the request body
    OkHttpClient client = defaultClient(groqApiKey, Duration.ofSeconds(60))
        .newBuilder()
        .addInterceptor(logging)
        .addInterceptor(new MyInterceptor())
        .build();
    Retrofit retrofit = new Retrofit.Builder().baseUrl(GROQ_URL)
        .client(client)
        .addConverterFactory(JacksonConverterFactory.create(mapper))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build();
    this.service = new OpenAiService(retrofit.create(OpenAiApi.class));
  }

  @Override
  public GenericChatMessage chat(String message, Map<String, Object> context) {
    ChatSession<ChatMessage, ChatFunctionCall> session = new ChatSession<>(backend, context, systemPrompt, bindings);
    ChatMessage chatMessage = new UserMessage(message);
    session.addMessage(chatMessage);

    int retryCount = 0;
    while (true) {
      log.info("Calling GROQ with model {}", model.getModelName());
      ContextWindow<ChatMessage> contextWindow = session.getContextWindow();
      log.debug("Calling GROQ with messages: {}", contextWindow.getMessages());
      ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
          .builder()
          .model(model.getModelName())
          .messages(contextWindow.getMessages())
          .functions(contextWindow.getFunctions())
          .functionCall("auto")
          .n(1)
          .temperature(0.2)
          .maxTokens(model.getCompletionLength())
          .logitBias(new HashMap<>())
          .build();
      AssistantMessage responseMessage;
      try {
        responseMessage = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage();
      } catch (OpenAiHttpException e) {
        // Workaround for groq API bug that throws 400 on some function calls
        if (e.statusCode == 400 && errorFunctionCall != null) {
          responseMessage = new AssistantMessage("", "", null, errorFunctionCall);
          errorFunctionCall = null;
        } else {
          throw e;
        }
      }
      log.info("Response:\n{}", responseMessage);
      String res = responseMessage.getTextContent();
      // Workaround for openai4j who doesn't recognize some function calls
      if (res != null) {
        String responseText = res.trim();
        if (responseText.startsWith("{\"function\"") && responseMessage.getFunctionCall() == null) {
          ChatFunctionCall functionCall = getFunctionCallFromText(responseText).orElse(null);
          if (functionCall != null) {
            responseMessage = new AssistantMessage("", functionCall.getName(), null, functionCall);
            log.info("!!!Remapped content to function call");
          }
        }
      }
      GenericChatMessage genericResponse = session.addMessage(responseMessage);
      ChatFunctionCall functionCall = responseMessage.getFunctionCall();
      if (functionCall != null) {
        ChatSession.FunctionExecutionOutcome<ChatMessage> outcome = session.validateAndExecuteFunctionCall(functionCall, true);
        switch (outcome.status()) {
          case EXECUTE_ON_CLIENT -> {
            return genericResponse;
          }
          case VALIDATION_ERROR_RETRY -> {
            if (retryCount >= ChatClientProvider.FUNCTION_CALL_RETRIES_LIMIT) {
              throw new RuntimeException("Too many function call retries for the same function.");
            } else {
              retryCount++;
              log.debug("Failed function call: {}", functionCall);
              log.info("Function call failed. Retry attempt #{}", retryCount + " ...");
            }
          }
        }
      } else {
        // The text answer
        return genericResponse;
      }
    }
  }

  // Workaround for groq API bug that throws 400 on some function calls
  class MyInterceptor implements Interceptor {
    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
      Request request = chain.request();
      Response response = chain.proceed(request);
      ResponseBody body = response.body();
      int code = response.code();
      if (code == 400 && body != null && body.contentType() != null && body.contentType().subtype() != null
          && body.contentType().subtype().toLowerCase().equals("json")) {
        BufferedSource source = body.source();
        source.request(Long.MAX_VALUE); // Buffer the entire body.
        Buffer buffer = source.buffer();
        Charset charset = body.contentType().charset(StandardCharsets.UTF_8);
        // Clone the existing buffer is they can only read once so we still want to pass
        // the original one to the chain.
        String jsonText = buffer.clone().readString(charset);
        errorFunctionCall = getFunctionCallFromGroqError(jsonText);
        if (errorFunctionCall != null) {
          log.info("!!!Extracted function call from 400");
        }
      }
      return response;
    }
  }

  private ChatFunctionCall getFunctionCallFromGroqError(String errorText) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode json = mapper.readTree(errorText);
      String failedGeneration = json.get("error").get("failed_generation").asText().trim();
      int startJson = failedGeneration.indexOf("{");
      int endJson = failedGeneration.lastIndexOf("}");
      String jsonText = failedGeneration.substring(startJson, endJson + 1);
      json = mapper.readTree(jsonText);
      JsonNode toolJson = json.get("tool_calls").get(0);
      return new ChatFunctionCall(toolJson.get("function").get("name").asText(), toolJson.get("parameters"));
    } catch (JsonProcessingException e) {
      log.error("Could not parse groq error [{}]:\n", errorText, e);
      return null;
    }
  }

  public static Optional<ChatFunctionCall> getFunctionCallFromText(String text) {
    Optional<JsonNode> functionCall = JsonUtil.parseJson(text);
    if (functionCall.isEmpty()) {
      log.error("Could not parse function text [{}]:\n", text);
      return Optional.empty();
    } else {
      return functionCall.map(json -> new ChatFunctionCall(json.get("function").asText(), json.get("parameters")));
    }
  }
}