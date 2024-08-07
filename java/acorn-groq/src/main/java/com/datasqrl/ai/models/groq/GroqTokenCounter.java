package com.datasqrl.ai.models.groq;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import com.datasqrl.ai.tool.FunctionDefinition;
import com.datasqrl.ai.models.ModelAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record GroqTokenCounter(HuggingFaceTokenizer tokenizer) implements ModelAnalyzer<ChatMessage> {

  @Override
  public int countTokens(ChatMessage message) {
    int numTokens = countTokens(message.getTextContent());
    return numTokens + numTokens / 10; //Add a 10% buffer
  }

  public int countTokens(String message) {
    if (message == null) {
      return 0;
    }
    return tokenizer.encode(message).getTokens().length;
  }

  @SneakyThrows
  @Override
  public int countTokens(FunctionDefinition function) {
    ObjectMapper mapper = new ObjectMapper();
    String jsonString = mapper.writeValueAsString(function);
    return countTokens(jsonString);
  }

  public static GroqTokenCounter of(GroqModelConfiguration modelConfig) {
    try {
      HuggingFaceTokenizer huggingFaceTokenizer = HuggingFaceTokenizer.newInstance(modelConfig.getTokenizerName());
      return new GroqTokenCounter(huggingFaceTokenizer);
    } catch (Exception e) {
      log.warn("Unrecognized tokenizer name: {}. Using [{}] tokenizer as backup.",
          modelConfig.getTokenizerName(), GroqModelConfiguration.DEFAULT_MODEL.getTokenizerName());
      return new GroqTokenCounter(HuggingFaceTokenizer.newInstance(GroqModelConfiguration.DEFAULT_MODEL.getTokenizerName()));
    }
  }
}
