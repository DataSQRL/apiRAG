# Configuration Utilities

Contains the [AcornAgentConfiguration](src/main/java/com/datasqrl/ai/config/AcornAgentConfiguration.java) class which configures and instantiates a model, provider, and tools backend from a configuration file.
Also contains utilities for configuring data visualization functions that get executed on the client and other shared configuration utilities.

## Configuration File

Here is an example configuration file for an agent:

```json
{
  "prompt": "This is the system prompt that tells the LLM how to act.",
  "functions": ["CurrentTime"],
  "model" : {
    "provider" : "openai",
    "name": "gpt-3.5-turbo",
    "temperature": 1.5,
    "max_output_tokens": 512
  },
  "apis": {
    "type": "rest",
    "url": "https://bored-api.appbrewery.com"
  }
}
```

The top level configuration options are:

| Field Name   | Descriptions                                                                                                                                                                                                                                                                                                                  | Required? | Default |
|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|---------|
| `prompt`     | The system prompt for the LLM                                                                                                                                                                                                                                                                                                 | Yes       | -       |
| `functions`  | An array of built-in function names to include into the tools. Names are case sensitive. See [Built-in functions](../acorn-udf/src/main/java/com/datasqrl/ai/function/builtin) for more details. Alternatively, client function can be loaded from a resource file that ends in `.json` - specify the relative resource path. | No        | none    |
| `context`    | An array of context keys that specify the session context parameters used to pass user, session, or other secure information to tools and for storing messages.                                                                                                                                                               | No        | none    |

## Model Configuration

Configures the model and provider to use by the agent.

| Field Name          | Descriptions                                                    | Required? | Default                            |
|---------------------|-----------------------------------------------------------------|-----------|------------------------------------|
| `name`              | Model name as used by the provider                              | Yes       | -                                  |
| `provider`          | Name of the provider (e.g. `openai`, `bedrock`, `google`, etc). | Yes       | -                                  |
| `max_input_tokens`  | The maximum number of input tokens that this LLM can support    | No        | inferred based on configured model |
| `max_output_tokens` | The maximum number of output tokens that this LLM can support   | No       | inferred based on configured model |
| `temperature`       | The temperature to use with this model                          | No       | 0.5                                |
| `top_p`             | The Top-P value to use with this model                          | No       | 0.9                                |
| `tokenizer`         | The HuggingFace tokenizer to use for counting tokens            | No       | inferred based on configured model |


Different model providers also require/offer other configuration parameters. (For example, Amazon Bedrock requires the `region` parameter). For all model configuration options, see the individual provider implementations for details.

## API Configuration

You can either configure a single API (which will have the name `default`) as shown in the example above or multiple APIs by name as shown in the example below :

```json
{
  "apis": {
    "api1": {
      "type": "graphql",
      "url": "https://localhost:8080/graphql"
    },
    "api2": {
      "type": "rest",
      "url": "https://localhost:8080/rest"
    }
  }
}
```
This configures two API endpoints to use by Acorn Agent with the names `api1` and `api2`.

| Field Name | Descriptions                                             | Required? | Default                            |
|------------|----------------------------------------------------------|-----------|------------------------------------|
| `type`     | The type of API, i.e one of `graphql`, `rest`, or `jdbc` | Yes       | -                                  |
| `url`      | The URL for the API                                      | Yes       | -                                  |
| `auth`     | Authentication headers for the API                       | No        | -                                  |
 
Additional configuration options may be used by the specific APIExecutor implementation.