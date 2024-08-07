package com.datasqrl.ai.config;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ClientSideFunctions {

  /**
   * For drawing one-dimensional charts on the client
   */
  datacharts("ui/datacharts.json"),
  /**
   * For drawing two-dimensional charts on the client
   */
  datacharts2d("ui/datacharts2d.json");

  private final String resourceFile;

  public static Optional<ClientSideFunctions> forName(String name) {
    for (ClientSideFunctions clientSideFunction : ClientSideFunctions.values()) {
      if (clientSideFunction.name().equalsIgnoreCase(name)) {
        return Optional.of(clientSideFunction);
      }
    }
    return Optional.empty();
  }



}
