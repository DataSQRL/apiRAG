package com.datasqrl.ai.models.openai;

import com.datasqrl.ai.tool.FunctionDefinition;
import com.datasqrl.ai.models.ModelAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public record OpenAITokenCounter(Encoding encoding) implements ModelAnalyzer<ChatMessage> {

  @Override
  public int countTokens(ChatMessage message) {
    int numTokens = countTokens(message.getTextContent());
    return numTokens + numTokens / 10; //Add a 10% buffer
  }

  public int countTokens(String message) {
    return encoding.countTokens(message);
  }

//TODO: This method is the same in every token counter. Move this logic to the caller and just call countTokens(String)
  @Override
  @SneakyThrows
  public int countTokens(FunctionDefinition function) {
    ObjectMapper mapper = new ObjectMapper();
    String jsonString = mapper.writeValueAsString(function);
    return countTokens(jsonString);
  }

  public static OpenAITokenCounter of(OpenAIModelConfiguration modelConfig) {
    Optional<EncodingType> encodingType = EncodingType.fromName(modelConfig.getTokenizerName());
    if (encodingType.isEmpty()) {
      log.warn("Unrecognized tokenizer name: {}. Using [{}] tokenizer as backup.",
          modelConfig.getTokenizerName(), OpenAIModelConfiguration.DEFAULT_MODEL.getEncodingType().getName());
      return new OpenAITokenCounter(Encodings.newDefaultEncodingRegistry().getEncodingForModel(OpenAIModelConfiguration.DEFAULT_MODEL));
    } else {
      return new OpenAITokenCounter(Encodings.newDefaultEncodingRegistry().getEncoding(encodingType.get()));
    }
  }
}
