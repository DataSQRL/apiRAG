package com.datasqrl.ai.spring;

import com.datasqrl.ai.models.bedrock.BedrockChatMessage;
import com.datasqrl.ai.models.bedrock.BedrockChatRole;
import com.datasqrl.ai.models.bedrock.BedrockFunctionCall;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

import java.time.Instant;

/**
 * TODO: Replace this by returning a GenericChatMessage from the ChatClientProvider
 * and then mapping the generic message to a reponse.
 */
public class ProviderMessageMapper {

  public static ResponseMessage toResponse(Object msg) {
    if (msg instanceof ChatMessage) {
      return toResponse((ChatMessage) msg);
    } else if (msg instanceof BedrockChatMessage) {
      return toResponse((BedrockChatMessage) msg);
    } else {
      throw new IllegalArgumentException("Unexpected chat message: " + msg.getClass());
    }
  }


  public static ResponseMessage toResponse(ChatMessage msg) {
    ChatFunctionCall functionCall = null;
    if (ChatMessageRole.valueOf(msg.getRole().toUpperCase()) == ChatMessageRole.ASSISTANT) {
      functionCall = ((AssistantMessage) msg).getFunctionCall();
    }
    if (functionCall != null) {
      System.out.println(functionCall.getArguments());
      return new ResponseMessage(msg.getRole(), null,
          functionCall.getArguments(), "", Instant.now().toString());
    } else {
      return new ResponseMessage(msg.getRole(),
          msg.getTextContent(),
          null,
          "",
          Instant.now().toString()
      );
    }
  }

  public static ResponseMessage toResponse(BedrockChatMessage msg) {
    BedrockFunctionCall functionCall = null;
    if (msg.getRole() == BedrockChatRole.ASSISTANT) {
      functionCall = msg.getFunctionCall();
    }
    if (functionCall != null) {
      System.out.println(functionCall.getArguments());
      return new ResponseMessage(msg.getRole().getRole(), null,
          functionCall.getArguments(), "", Instant.now().toString());
    } else {
      return new ResponseMessage(msg.getRole().getRole(),
          msg.getTextContent(),
          null,
          "",
          Instant.now().toString()
      );
    }
  }

}
