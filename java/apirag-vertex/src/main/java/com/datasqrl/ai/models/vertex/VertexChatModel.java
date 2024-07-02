package com.datasqrl.ai.models.vertex;

import com.datasqrl.ai.models.GenericLanguageModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum VertexChatModel implements GenericLanguageModel {

  GEMINI_1_5_FLASH("gemini-1.5-flash", 1000000, 8192),
  GEMINI_1_5_PRO("gemini-1.5-pro", 1000000, 8192);

  final String modelName;
  final int contextWindowLength;
  final int completionLength;
}
