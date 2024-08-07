package com.datasqrl.ai.models.vertex;

import com.datasqrl.ai.models.ModelAnalyzer;
import com.datasqrl.ai.tool.FunctionDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record VertexTokenCounter(GenerativeModel model) implements ModelAnalyzer<Content> {

  @SneakyThrows
  @Override
  public int countTokens(Content content) {
    int numTokens = model.countTokens(content).getTotalTokens();
    return numTokens + numTokens / 10; //Add a 10% buffer
  }

  //TODO: This method is the same in every token counter. Move this logic to the caller and just call countTokens(String)
  @SneakyThrows
  @Override
  public int countTokens(FunctionDefinition function) {
    ObjectMapper mapper = new ObjectMapper();
    String jsonString = mapper.writeValueAsString(function);
    return countTokens(jsonString);
  }

  @SneakyThrows
  public int countTokens(String message) {
    return model.countTokens(message).getTotalTokens();
  }

  public static VertexTokenCounter of(VertexModelConfiguration modelConfig) {
    VertexAI vertexAI = new VertexAI(modelConfig.getProjectId(), modelConfig.getLocation());
    try {
      GenerativeModel model = new GenerativeModel(modelConfig.getTokenizerName(), vertexAI);
      return new VertexTokenCounter(model);
    } catch (Exception e) {
      log.warn("Unrecognized model name: {}. Using [{}] model for tokenizing as backup.",
          modelConfig.getTokenizerName(), VertexModelConfiguration.DEFAULT_MODEL.getModelName());
      return new VertexTokenCounter(new GenerativeModel(VertexModelConfiguration.DEFAULT_MODEL.getModelName(), vertexAI));
    }
  }
}
