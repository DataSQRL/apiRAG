package com.datasqrl.ai.models.groq;

import com.datasqrl.ai.models.AbstractModelConfiguration;

import java.util.Optional;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;

@Slf4j
@Value
public class GroqModelConfiguration extends AbstractModelConfiguration {

  public static final GroqModelType DEFAULT_MODEL = GroqModelType.LLAMA3_8B;

  GroqModelType modelType;

  public GroqModelConfiguration(Configuration configuration) {
    super(configuration);
    Optional<GroqModelType> modelType = GroqModelType.fromName(super.getModelName());
    if (modelType.isEmpty()) {
      log.warn("Unrecognized model name: {}. Using [{}] model for token sizing.", super.getModelName(), DEFAULT_MODEL.getModelName());
      this.modelType = DEFAULT_MODEL;
    } else {
      this.modelType = modelType.get();
    }
  }

  @Override
  protected int getMaxTokensForModel() {
    return modelType.getContextWindowLength();
  }

  @Override
  public String getTokenizerName() {
    return configuration.getString(AbstractModelConfiguration.TOKENIZER_KEY, modelType.getTokenizerName());
  }

}
