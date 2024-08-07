# Rick and Morty API

Uses the [Rick and Morty API](https://rickandmortyapi.com/) to answer questions about your favorite TV show.

## Run the ChatBot

Run the ChatBot server using the following command (make sure to replace `{ADD_YOUR_KEY}` with your OpenAI key):
```
docker run -it --rm -p 8080:8080 -v $PWD:/config/ -e OPENAI_API_KEY={ADD_YOUR_KEY} datasqrl/acorn:latest /config/rickandmorty.config.json /config/rickandmorty.tools.json
```

Then [open the ChatBot](http://localhost:8080/?login=false) in your browser to interact with the chatbot.

To use Google Vertex as the model provider with Gemini 1.5 Flash as the model, simply change the configuration file to `rickandmorty.google-gemini.config.json`.
In general, you can change the model provider and LLM by updating the `model` section in the configuration file.