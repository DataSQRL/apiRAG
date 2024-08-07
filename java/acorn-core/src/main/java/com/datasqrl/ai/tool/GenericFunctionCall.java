package com.datasqrl.ai.tool;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenericFunctionCall {

  String name;
  JsonNode arguments;

  public static class JacksonSerializer extends JsonSerializer<GenericFunctionCall> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void serialize(GenericFunctionCall value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      gen.writeString(objectMapper.writeValueAsString(value));
    }

  }

  public static class JacksonDeserializer extends JsonDeserializer<GenericFunctionCall> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public GenericFunctionCall deserialize(JsonParser jsonParser,
        DeserializationContext deserializationContext) throws IOException {
      return objectMapper.readValue(jsonParser.getValueAsString(), GenericFunctionCall.class);
    }
  }


}
