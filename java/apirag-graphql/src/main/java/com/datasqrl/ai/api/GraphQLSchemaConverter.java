package com.datasqrl.ai.api;

import static graphql.Scalars.GraphQLString;

import com.datasqrl.ai.backend.FunctionDefinition;
import com.datasqrl.ai.backend.FunctionDefinition.Argument;
import com.datasqrl.ai.backend.FunctionDefinition.Parameters;
import com.datasqrl.ai.backend.FunctionType;
import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.datasqrl.ai.util.ErrorHandling;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.SchemaPrinter;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Converts a given GraphQL Schema to a tools configuration for the function backend.
 * It extracts all queries and mutations and converts them into {@link com.datasqrl.ai.backend.RuntimeFunctionDefinition}.
 */
@Value
@Slf4j
public class GraphQLSchemaConverter {

  Configuration configuration;
  String apiName;

  SchemaPrinter schemaPrinter = new SchemaPrinter();

  public List<RuntimeFunctionDefinition> convert(String schemaString) {
    TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(schemaString);
    RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().build();
    GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring);

    List<RuntimeFunctionDefinition> functions = new ArrayList<>();

    GraphQLObjectType queryType = graphQLSchema.getQueryType();
    GraphQLObjectType mutationType = graphQLSchema.getMutationType();
    Stream.concat(queryType.getFieldDefinitions().stream().map(fieldDef -> Pair.of("query", fieldDef))
            ,mutationType.getFieldDefinitions().stream().map(fieldDef -> Pair.of("mutation", fieldDef)))
        .flatMap(input -> {
      try {
        return Stream.of(convert(input.getKey(), input.getValue()));
      } catch (Exception e) {
        log.error("Error converting query: {}", input.getValue(), e);
        return Stream.of();
      }
    }).forEach(functions::add);
    return functions;
  }

  private static record Context(String prefix, int numArgs) {}

  public RuntimeFunctionDefinition convert(String prefix, GraphQLFieldDefinition fieldDef) {
    FunctionDefinition funcDef = new FunctionDefinition();
    Parameters params = new Parameters();
    params.setType("object");
    params.setProperties(new HashMap<>());
    params.setRequired(new ArrayList<>());
    funcDef.setDescription(fieldDef.getDescription());
    funcDef.setName(fieldDef.getName());
    funcDef.setParameters(params);

    StringBuilder queryHeader = new StringBuilder(prefix).append(" ").append(fieldDef.getName()).append("(");
    StringBuilder queryBody = new StringBuilder();

    visit(fieldDef, queryBody, queryHeader, params, new Context("", 0));

    queryHeader.append(") {\n").append(queryBody).append("\n}");
    APIQuery apiQuery = new APIQuery();
    apiQuery.setQuery(queryHeader.toString());
    apiQuery.setName(apiName);
    return RuntimeFunctionDefinition.builder()
        .type(FunctionType.api)
        .function(funcDef)
        .api(apiQuery)
        .build();
  }

  private static String combineStrings(String prefix, String suffix) {
    return prefix + (prefix.isBlank()? "" : "_") + suffix;
  }


  private static record UnwrappedType(GraphQLInputType type, boolean required) {}

  private UnwrappedType convertRequired(GraphQLInputType type) {
    boolean required = false;
    if (type instanceof GraphQLNonNull) {
      required = true;
      type = (GraphQLInputType) ((GraphQLNonNull) type).getWrappedType();
    }
    return new UnwrappedType(type, required);
  }

  private Argument convert(GraphQLInputType graphQLInputType) {
    Argument argument = new Argument();
    if (graphQLInputType instanceof GraphQLScalarType) {
      argument.setType(convertScalarTypeToJsonType((GraphQLScalarType) graphQLInputType));
    } else if (graphQLInputType instanceof GraphQLEnumType) {
      GraphQLEnumType enumType = (GraphQLEnumType) graphQLInputType;
      argument.setType("string");
      argument.setEnumValues(enumType.getValues().stream().map(GraphQLEnumValueDefinition::getName).collect(Collectors.toSet()));
    } else if (graphQLInputType instanceof GraphQLList) {
      argument.setType("array");
      argument.setItems(convert(convertRequired((GraphQLInputType) ((GraphQLList) graphQLInputType).getWrappedType()).type()));
    } else {
      throw new UnsupportedOperationException("Unsupported type: " + graphQLInputType);
    }
    return argument;
  }

  public String convertScalarTypeToJsonType(GraphQLScalarType scalarType) {
    return switch (scalarType.getName()) {
      case "Int" -> "integer";
      case "Float" -> "number";
      case "String" -> "string";
      case "Boolean" -> "boolean";
      case "ID" -> "string"; // Typically treated as a string in JSON Schema
      default -> throw new IllegalArgumentException("Unknown scalar type: " + scalarType.getName());
    };
  }


  public void visit(GraphQLFieldDefinition fieldDef, StringBuilder queryBody, StringBuilder queryHeader,
      Parameters params, Context ctx) {
    queryBody.append(fieldDef.getName());
    int numArgs = 0;
    if (!fieldDef.getArguments().isEmpty()) {
      queryBody.append("(");
      for (GraphQLArgument arg : fieldDef.getArguments()) {
        UnwrappedType unwrappedType = convertRequired(arg.getType());
        if (unwrappedType.type() instanceof GraphQLInputObjectType) {
          GraphQLInputObjectType inputType = (GraphQLInputObjectType) unwrappedType.type();
          for (GraphQLInputObjectField nestedField : inputType.getFieldDefinitions()) {
            String argName = combineStrings(ctx.prefix(), nestedField.getName());
            String description = nestedField.getDescription();
            unwrappedType = convertRequired(nestedField.getType());
            Argument argDef = convert(unwrappedType.type());
            argDef.setDescription(description);
            if (numArgs>0) queryBody.append(", ");
            if (ctx.numArgs() + numArgs > 0) queryHeader.append(", ");
            if (unwrappedType.required()) params.getRequired().add(argName);
            params.getProperties().put(argName, argDef);
            argName = "$" + argName;
            queryBody.append(nestedField.getName()).append(": ").append(argName);
            String typeString = printFieldType(nestedField);
            queryHeader.append(argName).append(": ").append(typeString);
            numArgs++;
          }
        } else {
          String argName = combineStrings(ctx.prefix(), arg.getName());
          String description = arg.getDescription();
          Argument argDef = convert(unwrappedType.type());
          argDef.setDescription(description);
          if (numArgs>0) queryBody.append(", ");
          if (ctx.numArgs() + numArgs > 0) queryHeader.append(", ");
          if (unwrappedType.required()) params.getRequired().add(argName);
          params.getProperties().put(argName, argDef);
          argName = "$" + argName;
          queryBody.append(arg.getName()).append(": ").append(argName);
          String typeString = printArgumentType(arg);
          queryHeader.append(argName).append(": ").append(typeString);
          numArgs++;
        }
      }
      queryBody.append(")");
    }
    GraphQLOutputType type = unwrapType(fieldDef.getType());
    if (type instanceof GraphQLObjectType) {
      queryBody.append(" {\n");
      for (GraphQLFieldDefinition nestedField : ((GraphQLObjectType)type).getFieldDefinitions()) {
        visit(nestedField, queryBody, queryHeader, params, new Context(combineStrings(ctx.prefix(), nestedField.getName()), ctx.numArgs() + numArgs));
      }
      queryBody.append("}\n");
    } else {
      queryBody.append("\n");
    }
  }

  private String printFieldType(GraphQLInputObjectField field) {
    GraphQLInputObjectType type = GraphQLInputObjectType.newInputObject()
        .name("DummyType")
        .field(field)
        .build();
    // Print argument as part of a dummy field in a dummy schema
    String output = schemaPrinter.print(type);
    return extractTypeFromDummy(output, field.getName());
  }

  private String printArgumentType(GraphQLArgument argument) {
    GraphQLObjectType type = GraphQLObjectType.newObject()
        .name("DummyType")
        .field(field -> field
            .name("dummyField")
            .type(GraphQLString)
            .argument(argument)
        )
        .build();
    // Print argument as part of a dummy field in a dummy schema
    String output = schemaPrinter.print(type);
    return extractTypeFromDummy(output, argument.getName());
  }

  private String extractTypeFromDummy(String output, String fieldName) {
    Pattern pattern = Pattern.compile(fieldName + "\\s*:\\s*([^)}]+)");
    // Print argument as part of a dummy field in a dummy schema
    Matcher matcher = pattern.matcher(output);
    ErrorHandling.checkArgument(matcher.find(), "Could not find type in: %s", output);
    return matcher.group(1).trim();
  }

  private static GraphQLOutputType unwrapType(GraphQLOutputType type) {
    if (type instanceof GraphQLList) {
      return unwrapType((GraphQLOutputType) ((GraphQLList) type).getWrappedType());
    } else if (type instanceof GraphQLNonNull) {
      return unwrapType((GraphQLOutputType)((GraphQLNonNull) type).getWrappedType());
    } else {
      return type;
    }
  }

}