# Weather Forecast

Uses the [Open Meteo API](https://open-meteo.com/) to retrieve weather forecast information. The chatbot automatically determines if and how to visualize the data.

## Run the ChatBot

Run the ChatBot server using the following command (make sure to replace `{ADD_YOUR_KEY}` with your OpenAI key):
```
docker run -it --rm -p 8080:8080 -v $PWD:/config/ -e OPENAI_API_KEY={ADD_YOUR_KEY} datasqrl/sqrl-apirag /config/weather.config.json /config/weather.tools.json
```

Then [open the ChatBot](http://localhost:8080/?login=false) in your browser to interact with the chatbot.