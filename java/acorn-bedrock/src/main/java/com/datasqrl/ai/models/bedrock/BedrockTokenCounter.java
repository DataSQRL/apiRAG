package com.datasqrl.ai.models.bedrock;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import com.datasqrl.ai.tool.FunctionDefinition;
import com.datasqrl.ai.models.ModelAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

// Added `BedrockTokenCounter` in order to keep the class separation, although it's the same as the `GroqTokenCounter`.
// Think of merging these two into a `HuggingFaceTokenCounter` in the future
@Slf4j
public record BedrockTokenCounter(HuggingFaceTokenizer tokenizer) implements ModelAnalyzer<BedrockChatMessage> {

  @Override
  public int countTokens(BedrockChatMessage message) {
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

  public static BedrockTokenCounter of(BedrockModelConfiguration modelConfig) {
    try {
      HuggingFaceTokenizer huggingFaceTokenizer = HuggingFaceTokenizer.newInstance(modelConfig.getTokenizerName());
      return new BedrockTokenCounter(huggingFaceTokenizer);
    } catch (Exception e) {
      log.warn("Unrecognized tokenizer name: {}. Using [{}] tokenizer as backup.",
          modelConfig.getTokenizerName(), BedrockModelConfiguration.DEFAULT_MODEL.getTokenizerName());
      return new BedrockTokenCounter(HuggingFaceTokenizer.newInstance(BedrockModelConfiguration.DEFAULT_MODEL.getTokenizerName()));
    }
  }
}
