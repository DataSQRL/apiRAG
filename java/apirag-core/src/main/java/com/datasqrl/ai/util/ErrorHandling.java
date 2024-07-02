package com.datasqrl.ai.util;

public class ErrorHandling {

  public static void checkArgument(boolean condition, String message, Object... args) {
    if (!condition) {
      throw new IllegalArgumentException(String.format(message, args));
    }
  }

  public static void checkArgument(boolean condition) {
    checkArgument(condition, "Unexpected arguments in method invocation");
  }

  public static void checkNotNullOrEmpty(String s, String message, Object... args) {
    ErrorHandling.checkArgument( s!=null && !s.isBlank(), message, args);
  }


}
