package dk.elkjaerit.smartheating.weather;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dk.elkjaerit.smartheating.common.model.Building;
import lombok.Builder;
import lombok.Value;
import net.e175.klaus.solarpositioning.AzimuthZenithAngle;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class OpenWeatherMapClient {
  private static final Logger logger = Logger.getLogger(OpenWeatherMapClient.class.getName());
  private static final Gson gson = new Gson();
  private static final Map<String, WeatherMetaData> cache = new HashMap<>();

  public static Weather getCurrentWeather(Building building) {
    String cityId = building.getCityId();
    if (cache.containsKey(cityId) && cache.get(cityId).getExpire().isAfter(LocalDateTime.now())) {
      logger.info("Using cached value");
      return cache.get(cityId).getWeather();
    } else {
      logger.info("Calculate new weather data");
      Weather weather = getWeather(building);
      cache.put(
          cityId,
          WeatherMetaData.builder()
              .weather(weather)
              .expire(LocalDateTime.now().plusMinutes(10))
              .build());
      return weather;
    }
  }

  public static List<WeatherForecast> getForecast(Building building)
      throws IOException, InterruptedException {
    var client = HttpClient.newHttpClient();
    var httpRequest =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    String.format(
                        "https://api.openweathermap.org/data/2.5/forecast?id="
                            + building.getCityId()
                            + "&appid=%s&units=metric&cnt=10",
                        "8265eb298a6155bd31cffccc8695625a")))
            .GET()
            .build();
    var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    JsonElement requestParsed = gson.fromJson(response.body(), JsonElement.class);
    JsonObject requestJson = requestParsed.getAsJsonObject();

    List<WeatherForecast> result = new ArrayList<>();

    JsonArray list = requestJson.get("list").getAsJsonArray();

    list.forEach(
        jsonElement -> {
          JsonObject current = jsonElement.getAsJsonObject();
          ZonedDateTime dt =
              ZonedDateTime.ofInstant(
                  Instant.ofEpochSecond(current.get("dt").getAsLong()),
                  ZoneId.of("Europe/Copenhagen"));
          AzimuthZenithAngle azimuthAndZenithAngle =
              Sun.getAzimuthAndZenithAngle(
                  building.getLocation().getLatitude(), building.getLocation().getLongitude(), dt);

          WeatherForecast weatherForecast =
              WeatherForecast.builder()
                  .dateTime(dt)
                  .cloudCover(current.getAsJsonObject("clouds").get("all").getAsDouble())
                  .temp(current.getAsJsonObject("main").get("temp").getAsDouble())
                  .windDirection(current.getAsJsonObject("wind").get("deg").getAsInt())
                  .windSpeed(current.getAsJsonObject("wind").get("speed").getAsDouble())
                  .azimuth(azimuthAndZenithAngle.getAzimuth())
                  .zenith(azimuthAndZenithAngle.getZenithAngle())
                  .build();
          result.add(weatherForecast);
        });

    return result;
  }

  private static Weather getWeather(Building building) {
    var client = HttpClient.newHttpClient();
    var httpRequest =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    String.format(
                        "https://api.openweathermap.org/data/2.5/weather?id="
                            + building.getCityId()
                            + "&appid=%s&units=metric",
                        "8265eb298a6155bd31cffccc8695625a")))
            .GET()
            .build();
    HttpResponse<String> response = null;
    try {
      response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Could not get weather");
    }
    JsonElement requestParsed = gson.fromJson(response.body(), JsonElement.class);
    JsonObject requestJson = requestParsed.getAsJsonObject();

    AzimuthZenithAngle azimuthAndZenithAngle =
        Sun.getAzimuthAndZenithAngle(
            building.getLocation().getLatitude(),
            building.getLocation().getLongitude(),
            ZonedDateTime.now(ZoneId.of("Europe/Copenhagen")));

    return Weather.builder()
        .temp(requestJson.getAsJsonObject("main").get("temp").getAsDouble())
        .cloudCover(requestJson.getAsJsonObject("clouds").get("all").getAsDouble())
        .windSpeed(requestJson.getAsJsonObject("wind").get("speed").getAsDouble())
        .windDirection(requestJson.getAsJsonObject("wind").get("deg").getAsInt())
        .azimuth(azimuthAndZenithAngle.getAzimuth())
        .zenith(azimuthAndZenithAngle.getZenithAngle())
        .build();
  }

  @Value
  @Builder
  private static class WeatherMetaData {
    LocalDateTime expire;
    Weather weather;
  }
}
