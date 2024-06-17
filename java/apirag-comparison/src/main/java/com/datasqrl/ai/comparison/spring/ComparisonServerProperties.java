package com.datasqrl.ai.comparison.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent")
@Data
public class ComparisonServerProperties {

  private String config;
  private String tools;

}
