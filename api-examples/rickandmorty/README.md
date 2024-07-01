# Rick and Morty API

Uses the [Rick and Morty API](https://rickandmortyapi.com/) to answer questions about your favorite TV show.

## Run the ChatBot

Run the ChatBot server using the following command (make sure to replace `{ADD_YOUR_KEY}` with your OpenAI key):
```
docker run -it --rm -p 8080:8080 -v $PWD:/config/ -e OPENAI_API_KEY={ADD_YOUR_KEY} datasqrl/sqrl-apirag /config/rickandmorty.config.json /config/rickandmorty.tools.json
```

Then [open the ChatBot](http://localhost:8080/?login=false) in your browser to interact with the chatbot.