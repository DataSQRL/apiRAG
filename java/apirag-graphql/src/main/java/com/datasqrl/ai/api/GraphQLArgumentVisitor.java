package com.datasqrl.ai.api;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLTypeVisitor;
import graphql.schema.GraphQLUnionType;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

public class GraphQLArgumentVisitor implements GraphQLTypeVisitor {

  @Override
  public TraversalControl visitGraphQLArgument(GraphQLArgument node,
      TraverserContext<GraphQLSchemaElement> context) {
    return null;
  }

  @Override
  public TraversalControl visitGraphQLDirective(GraphQLDirective node,
      TraverserContext<GraphQLSchemaElement> context) {
    return null;
  }

  @Override
  public TraversalControl visitGraphQLEnumType(GraphQLEnumType node,
      TraverserContext<GraphQLSchemaElement> context) {
    return null;
  }

  @Override
  public TraversalControl visitGraphQLEnumValueDefinition(GraphQLEnumValueDefinition node,
      TraverserContext<GraphQLSchemaElement> context) {
    return null;
  }

  @Override
  public TraversalControl visitGraphQLFieldDefinition(GraphQLFieldDefinition node,
      TraverserContext<GraphQLSchemaElement> context) {
    return null;
  }

  @Override
  public TraversalControl visitGraphQLInputObjectField(GraphQLInputObjectField node,
      TraverserContext<GraphQLSchemaElement> context) {

    return null;
  }

  @Override
  public TraversalControl visitGraphQLInputObjectType(GraphQLInputObjectType node,
      TraverserContext<GraphQLSchemaElement> context) {
    return null;
  }

  @Override
  public TraversalControl visitGraphQLInterfaceType(GraphQLInterfaceType node,
      TraverserContext<GraphQLSchemaElement> context) {
    return null;
  }

  @Override
  public TraversalControl visitGraphQLList(GraphQLList node,
      TraverserContext<GraphQLSchemaElement> context) {
    return null;
  }

  @Override
  public TraversalControl visitGraphQLNonNull(GraphQLNonNull node,
      TraverserContext<GraphQLSchemaElement> context) {
    return null;
  }

  @Override
  public TraversalControl visitGraphQLObjectType(GraphQLObjectType node,
      TraverserContext<GraphQLSchemaElement> context) {
    return null;
  }

  @Override
  public TraversalControl visitGraphQLScalarType(GraphQLScalarType node,
      TraverserContext<GraphQLSchemaElement> context) {
    return null;
  }

  @Override
  public TraversalControl visitGraphQLTypeReference(GraphQLTypeReference node,
      TraverserContext<GraphQLSchemaElement> context) {
    return null;
  }

  @Override
  public TraversalControl visitGraphQLUnionType(GraphQLUnionType node,
      TraverserContext<GraphQLSchemaElement> context) {
    return null;
  }
}
