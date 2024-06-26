package com.datasqrl.ai.models.bedrock;

import com.datasqrl.ai.models.ChatMessageEncoder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class Llama3MessageEncoder implements ChatMessageEncoder<BedrockChatMessage> {

  public String encodeMessage(BedrockChatMessage message) {
    return switch (message.getRole()) {
      case USER -> "<|start_header_id|>user<|end_header_id|>\n"
          + message.getTextContent() + "\n"
          + "<|eot_id|>";
      case ASSISTANT -> "<|start_header_id|>assistant<|end_header_id|>\n"
          + message.getTextContent() + "\n"
          + "<|eot_id|>";
      case FUNCTION -> "<|start_header_id|>function<|end_header_id|>\n"
          + message.getTextContent() + "\n"
          + "<|eot_id|>";
      case SYSTEM -> "<|begin_of_text|>\n"
          + "<|start_header_id|>system<|end_header_id|>\n"
          + message.getTextContent()
          + "<|eot_id|>";
    };
  }

  @Override
  public BedrockChatMessage decodeMessage(String text, String roleHint) {
    if (text.contains("<|start_header_id|>")
        && text.contains("<|end_header_id|>")) {
      int startRole = text.indexOf("<|start_header_id|>") + 19;
      int endRole = text.indexOf("<|end_header_id|>");
      int startContent = endRole + 17;
      String role = text.substring(startRole, endRole);
      String content = text.substring(startContent);
      String cleanText = content.replace("<|eot_id|>", "").trim();
      BedrockChatMessage message = new BedrockChatMessage(BedrockChatRole.valueOf(role.toUpperCase()), cleanText, "");
      return message;
    } else {
      log.error("Message can't be decoded:\n{}", text);
    }
    if (roleHint != null) {
      return new BedrockChatMessage(BedrockChatRole.valueOf(roleHint.toUpperCase()), text, "");
    }
    return new BedrockChatMessage(BedrockChatRole.USER, text, "");

  }
}
