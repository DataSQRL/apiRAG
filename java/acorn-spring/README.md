# Spring Boot

This module contains the infrastructure for building AI agents with the Spring web development framework. Include the `acorn-spring` in your Spring project as a dependency.

This module includes:
* **[API Executors](src/main/java/com/datasqrl/ai/api)**: Spring specific executors for GraphQL, REST, and JDBC.
* **[ChatBot Frontend](src/main/resources/public)**: Javascript implementation of a ChatBot frontend with custom data visualization.
* **Utility Classes**: `InputMessage` and `ResponseMessage` for the interface of a ChatBot implementation.

For an example of how to use this module to build an AI Agent with Spring Boot, take a look at the [acorn-server module](../acorn-server).