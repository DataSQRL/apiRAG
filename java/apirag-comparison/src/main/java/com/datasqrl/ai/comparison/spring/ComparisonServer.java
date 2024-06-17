package com.datasqrl.ai.comparison.spring;

import com.datasqrl.ai.comparison.config.ComparisonConfiguration;
import com.datasqrl.ai.models.ChatClientProvider;
import com.datasqrl.ai.spring.InputMessage;
import com.datasqrl.ai.spring.ResponseMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SpringBootApplication
@Slf4j
public class ComparisonServer {

  public static void main(String[] args) {
    try {
      SpringApplication.run(ComparisonServer.class, args);
    } catch (Exception e) {
      log.error("Could not start server", e);
    }
  }

  public static class MessageController {

    private final ChatClientProvider<?, ?> chatClientProvider;
    private final Function<String,Map<String,Object>> getContextFunction;

    @SneakyThrows
    public MessageController(ComparisonServerProperties props) {
      ComparisonConfiguration configuration = ComparisonConfiguration.fromFile(Path.of(props.getConfig()), Path.of(props.getTools()));
      this.getContextFunction = configuration.getContextFunction();
      this.chatClientProvider = configuration.getChatProvider();
    }

    @GetMapping("/messages")
    public List<ResponseMessage> getMessages(@RequestParam String userId) {
      Map<String, Object> context = getContextFunction.apply(userId);
      return chatClientProvider.getHistory(context, false).stream().map(ResponseMessage::from).toList();
    }

    @PostMapping("/messages")
    public ResponseMessage postMessage(@RequestBody InputMessage message) {
      log.info("\nUser #{}: {}", message.getUserId(), message.getContent());
      Map<String, Object> context = getContextFunction.apply(message.getUserId());
      return ResponseMessage.from(chatClientProvider.chat(message.getContent(), context));
    }
  }
}
