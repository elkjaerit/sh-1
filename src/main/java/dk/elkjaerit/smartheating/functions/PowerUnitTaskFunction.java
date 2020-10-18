package dk.elkjaerit.smartheating.functions;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dk.elkjaerit.smartheating.powerunit.PowerUnitUpdater;

import java.util.logging.Logger;

public class PowerUnitTaskFunction implements HttpFunction {
    private static final Logger logger = Logger.getLogger(PowerUnitTaskFunction.class.getName());

    private static final Gson gson = new Gson();
    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        try {
            JsonElement requestParsed = gson.fromJson(request.getReader(), JsonElement.class);
            JsonObject requestJson = null;

            if (requestParsed != null && requestParsed.isJsonObject()) {
                requestJson = requestParsed.getAsJsonObject();
            }

            if (requestJson != null && requestJson.has("buildingId")) {
                var buildingId = requestJson.get("buildingId").getAsString();
                PowerUnitUpdater.update(buildingId);
            }
        } catch (JsonParseException e) {
            logger.severe("Error parsing JSON: " + e.getMessage());
        }

        response.setStatusCode(200);
    }
}
