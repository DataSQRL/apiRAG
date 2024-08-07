package com.datasqrl.ai.spring;

import com.datasqrl.ai.config.AcornAgentConfiguration;
import com.datasqrl.ai.config.ContextConversion;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.ToolManager;
import com.datasqrl.ai.trace.Trace;
import com.datasqrl.ai.trace.TraceChatProvider;
import com.datasqrl.ai.trace.TraceContext;
import com.datasqrl.ai.trace.TraceRecordingToolManager;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@Slf4j
public class AcornAgentServer {

  public static void main(String[] args) {
    try {
      SpringApplication.run(AcornAgentServer.class, args);
    } catch (Exception e) {
      log.error("Application failed to start", e);
    }
  }

  @RestController
  public static class MessageController {

    private final AcornAgentConfiguration configuration;
    private final List<String> contextKeys;
    private final ToolManager toolsManager;

    private ChatProvider chatProvider;
    private Optional<Tracer> tracer;


    @SneakyThrows
    public MessageController(AcornAgentServerProperties props) {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(props.getConfig()),"Need to provide a configuration file");
      Preconditions.checkArgument(!Strings.isNullOrEmpty(props.getTools()), "Need to provide a tools file");
      this.configuration = AcornAgentConfiguration.fromFile(Path.of(props.getConfig()), Path.of(props.getTools()));
      this.contextKeys = configuration.getContext();
      this.toolsManager = configuration.getToolManager();

      if (props.isTracingEnabled()) {
        tracer = Optional.of(new Tracer());
        this.chatProvider = tracer.get().getChatProvider();
      } else {
        this.tracer = Optional.empty();
        this.chatProvider = configuration.getChatProvider(this.toolsManager);
      }
    }

    @GetMapping("/messages")
    public List<ResponseMessage> getMessages(@RequestParam String userId) {
      Context context = getContext(userId);
      return chatProvider.getHistory(context, false).stream().map(ResponseMessage::from).toList();
    }

    @PostMapping("/messages")
    public ResponseMessage postMessage(@RequestBody InputMessage message) {
      log.info("\nUser #{}: {}", message.getUserId(), message.getContent());
      if (tracer.isPresent() && message.getContent().equals("exit")) {
        String filename = tracer.get().saveTraceToFile();
        tracer = Optional.of(new Tracer());
        this.chatProvider = tracer.get().getChatProvider();
        return ResponseMessage.system(String.format("Trace written to file: %s. Session concluded.",filename));
      } else {
        Context context = getContext(message.getUserId());
        return ResponseMessage.from(chatProvider.chat(message.getContent(), context));
      }
    }

    private Context getContext(String userId) {
      Context context = ContextConversion.getContextFromUserId(userId, contextKeys);
      return tracer.map(t -> t.nextContext(context)).orElse(context);
    }

    private class Tracer {

      private final Trace.TraceBuilder traceBuilder = Trace.builder();
      private int requestIndex = 0;

      public Context nextContext(Context context) {
        TraceContext tContext = TraceContext.of(context.asMap());
        tContext.setRequestIndex(requestIndex++);
        return tContext;
      }

      public ChatProvider getChatProvider() {
        ToolManager tracingTools = new TraceRecordingToolManager(toolsManager, this.traceBuilder, Optional.empty());
        return new TraceChatProvider(configuration.getChatProvider(tracingTools), this.traceBuilder);
      }

      @SneakyThrows
      public String saveTraceToFile() {
        if (tracer.isEmpty()) {
          return null;
        }
        Trace trace = tracer.get().traceBuilder.build();
        String filename = String.format("trace_%s.json", System.currentTimeMillis()/1000);
        trace.writeToFile(Path.of(filename));
        return filename;
      }

    }

  }


}
