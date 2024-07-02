# Advice Monster

Uses the [Advice Slip API](https://api.adviceslip.com/) to dispense words of wisdom served with a small dose of toxic positivity.

## Run the ChatBot

Run the ChatBot server using the following command (make sure to replace `{ADD_YOUR_KEY}` with your OpenAI key):
```
docker run -it --rm -p 8080:8080 -v $PWD:/config/ -e OPENAI_API_KEY={ADD_YOUR_KEY} datasqrl/sqrl-apirag /config/advice.config.json /config/advice.tools.json
```

Then [open the ChatBot](http://localhost:8080/?login=false) in your browser to interact with the chatbot.