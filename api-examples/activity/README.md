# I'm Bored

Uses the [I'm Bored API](https://bored-api.appbrewery.com/) to encourage you to get your butt off the couch.

## Run the ChatBot

Run the ChatBot server using the following command (make sure to replace `{ADD_YOUR_KEY}` with your OpenAI key):
```
docker run -it --rm -p 8080:8080 -v $PWD:/config/ -e OPENAI_API_KEY={ADD_YOUR_KEY} datasqrl/sqrl-apirag /config/activity.openai.config.json /config/activity.tools.json
```

Then [open the ChatBot](http://localhost:8080/?login=false) in your browser to interact with the chatbot.

To use Groq as the model provider with Llama3 70B as the model, simply change the configuration file to `activity.groq-llama.config.json`.
In general, you can change the model provider and LLM by updating the `model` section in the configuration file.