# Java Implementation of Acorn Agent

This is the Java implementation of Acorn Agent which provides the libraries you need to implement an AI agent on the JVM.

## Getting Started

### Spring Boot

To build an AI agent as a web application with Spring Boot, include the following dependency in your project and use the generic implementation in [AcornAgentServer](acorn-spring/src/main/java/com/datasqrl/ai/spring/AcornAgentServer.java) as a starting point for your own.

#### Gradle

```text
dependencies {
    implementation 'com.datasqrl:acorn-spring:0.1.0'
}
```

#### Maven

```text
    <dependency>
        <groupId>com.datasqrl</groupId>
        <artifactId>acorn-spring</artifactId>
        <version>0.1.0</version>
    </dependency>
```

### Generic

If you prefer a different web development framework or want to use Acorn Agent in a different context (e.g. a command line application), use the following dependency and take a look at the [command line agent implementation examples](acorn-starter/src/test/java/com/datasqrl/ai/example) for inspiration.

#### Gradle

```text
dependencies {
    implementation 'com.datasqrl:acorn-starter:0.1.0'
}
```

#### Maven

```text
    <dependency>
        <groupId>com.datasqrl</groupId>
        <artifactId>acorn-starter</artifactId>
        <version>0.1.0</version>
    </dependency>
```

## Modules

The implementation is divided into multiple modules, so you can include exactly the modules you need in your agent implementation without pulling in unneeded dependencies.

* **[acorn-core](acorn-core/)**: The core module contains the implementation of the `ToolsBackend`, `ChatSession`, `ChatProvider`, and model configuration. Those are the core components of Acorn Agent.
* **[acorn-udf](acorn-udf)**: Adds support for user defined functions. Add this module if you want to add a tool that executes locally by invoking a function.
* **[acorn-openai](acorn-openai)**: Implements OpenAI as a model provider with support for the GPT-class of large-language models.
* **[acorn-bedrock](acorn-bedrock)**: Implements AWS Bedrock as a model provider with support for LLMs like Llama3 and others.
* **[acorn-vertex](acorn-vertex)**: Implements Google Vertex as a model provider with support for Gemini-class of LLMs and others.
* **[acorn-groq](acorn-groq)**: Implements Groq as a model provider with support for models like Llama3, Mixtral, etc.
* **[acorn-graphql](acorn-graphql)**: Supports mapping GraphQL schemas to tool configurations to simplify integration of GraphQL APIs as a tool.
* **[acorn-rest](acorn-rest)**: Utilities for calling REST APIs as a tool. Supports mapping OpenAPI schemas to tool configurations to simplify integration of REST APIs.
* **[acorn-config](acorn-config)**: Utilities for file-based configuration of Acorn Agent and it's components (i.e. models, provider, tools, and APIs)
* **[acorn-starter](acorn-starter)**: Sample API executors and example implementations of AI agents.
* **[acorn-spring](acorn-spring)**: Integrates Acorn Agent with the Spring Boot web development framework.

To include a module into your project, use a dependency management tool like Gradle or Maven as follows. Replace `[MODULE_NAME]` with the name of the module and configure `{acorn.version}` to the current version of Acorn Agent.

### Gradle

```text title='Gradle'
dependencies {
    implementation 'com.datasqrl:acorn-[MODULE_NAME]:${acorn.version}'
}
```

### Maven

```text title='Maven'
<dependencies>
    <dependency>
        <groupId>com.datasqrl</groupId>
        <artifactId>acorn-[MODULE_NAME]</artifactId>
        <version>${acorn.version}</version>
    </dependency>
</dependencies>
```