package com.datasqrl.ai.models.bedrock;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import com.datasqrl.ai.backend.FunctionDefinition;
import com.datasqrl.ai.backend.ModelAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.Value;

// Added `BedrockTokenCounter` in order to keep the class separation, although it's the same as the `GroqTokenCounter`.
// Think of merging these two into a `HuggingFaceTokenCounter` in the future
@Value
public class BedrockTokenCounter implements ModelAnalyzer<BedrockChatMessage> {

  HuggingFaceTokenizer tokenizer;

  @Override
  public int countTokens(BedrockChatMessage message) {
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

  public static BedrockTokenCounter of(BedrockChatModel model) {
    return new BedrockTokenCounter(HuggingFaceTokenizer.newInstance(model.tokenizerName));
  }
}
