package com.datasqrl.ai.models.groq;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import com.datasqrl.ai.backend.FunctionDefinition;
import com.datasqrl.ai.backend.ModelAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.SneakyThrows;
import lombok.Value;

@Value
public class GroqTokenCounter implements ModelAnalyzer<ChatMessage> {

  HuggingFaceTokenizer tokenizer;

  @Override
  public int countTokens(ChatMessage message) {
    int numTokens = countTokens(message.getTextContent());
    return numTokens + numTokens / 10; //Add a 10% buffer
  }

  @Override
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

  public static GroqTokenCounter of(GroqChatModel model) {
    return new GroqTokenCounter(HuggingFaceTokenizer.newInstance(model.tokenizerName));
  }
}
