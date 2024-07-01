package com.datasqrl.ai.api;

import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.datasqrl.ai.util.ErrorHandling;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.configuration2.PropertiesConfiguration;

public class GraphQLSchemaConverterExecutable {


  public static void main(String[] args) throws IOException {
    ErrorHandling.checkArgument(args.length>0, "Need to specify the filename of the GraphQL schema file");
    Path graphQLSchemaPath = Path.of(args[0]);
    ErrorHandling.checkArgument(Files.isRegularFile(graphQLSchemaPath), "Cannot access GraphQL schema file: %s", graphQLSchemaPath);
    String apiName = args.length>1 ? args[1] : APIExecutorFactory.DEFAULT_NAME;
    String graphQLSchema = Files.readString(graphQLSchemaPath);
    String inputFilename = graphQLSchemaPath.getFileName().toString();
    String outputFilename = inputFilename.substring(0, inputFilename.lastIndexOf(".")) + ".tools.json";
    String output = convert(graphQLSchema, apiName);
    Files.writeString(graphQLSchemaPath.getParent().resolve(outputFilename), output);
  }

  public static String convert(String graphQLSchema, String apiName) throws IOException {
    GraphQLSchemaConverter converter = new GraphQLSchemaConverter(
        new PropertiesConfiguration(),
        apiName);
    List<RuntimeFunctionDefinition> tools = converter.convert(graphQLSchema);
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.valueToTree(tools));
  }

}
