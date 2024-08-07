# Acorn Agent Server

This module contains a generic implementation of a ChatBot as a Spring Boot application with Acorn Agent. It allows you to run agents from configurations files without any coding required and is the basis of the Acorn Agent Docker image.

* **[AcornAgentServer](src/main/java/com/datasqrl/ai/spring)**: A spring boot server that reads a model, chat, and tools configuration from file to provide a ChatBot.

## Run the Spring Boot Application

First, compile the entire maven project in the [root folder](../) with:
```
mvn package
```

Run the `AcornAgentServer` with the configuration parameters and the API Keys
```
 java -DOPENAI_API_KEY=XXXX -jar acorn-spring/target/acorn-spring-0.1.0.jar --agent.config=[CONFIG_FILE] --agent.tools=[TOOLS_FILE] 
```
where CONFIG_FILE is a [configuration file](/java/acorn-config/) folder (e.g. [activity.openai.config.json](/examples/activity/activity.openai.config.json)) and TOOLS_FILE is a  [tools file](TOOLS_CONFIG.md) (e.g. [activity tools](/examples/activity/activity.tools.json)).

Note, that you have to pass in environmental variables for the specific model that you are using. Environmental variables need to be specified right after `java` in the command.