package dk.elkjaerit.smartheating;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import dk.elkjaerit.smartheating.common.model.Building;
import dk.elkjaerit.smartheating.common.model.DigitalOutput;
import dk.elkjaerit.smartheating.common.model.PredictionOverview;
import dk.elkjaerit.smartheating.common.model.Room;
import dk.elkjaerit.smartheating.ml.Predictor;
import dk.elkjaerit.smartheating.weather.OpenWeatherMapClient;
import dk.elkjaerit.smartheating.weather.WeatherForecast;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WeatherUpdater {
  private static final Logger LOGGER = Logger.getLogger(WeatherUpdater.class.getName());

  public static void main(String[] args) {
    run();
  }

  public static void run() {
    BuildingRepository.getBuildings().forEach(WeatherUpdater::updateBuilding);
  }

  private static void updateBuilding(QueryDocumentSnapshot buildingSnapshot) {
    LOGGER.info("Update building");
    try {
      Building building = buildingSnapshot.toObject(Building.class);
      WeatherForecast weatherForecast = getActualWeatherForecast(building, getPredictionTimer());
      LOGGER.info("Weather forecast: " + weatherForecast);
      buildingSnapshot
          .getReference()
          .collection("rooms")
          .get()
          .get()
          .getDocuments()
          .forEach(
              room -> {
                updateRoom(room, weatherForecast);
              });
    } catch (IOException | InterruptedException | ExecutionException e) {
      LOGGER.log(Level.SEVERE, "Error updating building", e);
    }
  }

  private static void updateRoom(
      QueryDocumentSnapshot roomQueryDocumentSnapshot, WeatherForecast weatherForecast) {

    Room room = roomQueryDocumentSnapshot.toObject(Room.class);
    if (room.getDigitalOutput() == null) {
      room.setDigitalOutput(DigitalOutput.builder().build());
    }
    LOGGER.info("Updating room : " + room.getName());

    PredictionOverview.Label predictedLabel = Predictor.predict(room, weatherForecast);
    LOGGER.info("Prediction: " + room.getName() + ": " + predictedLabel);

    double power;

    if (predictedLabel == PredictionOverview.Label.NEGATIVE) {
      room.getDigitalOutput().setPower(0);
    } else {
      power = calculateFromWeatherForecast(weatherForecast, room);
      double minimumPowerForRoom = room.getMinPower() != null ? room.getMinPower() : 0;
      double powerForRoom = Math.max(minimumPowerForRoom, Math.min(1, power));
      double adjustedForNight = adjustForNight(powerForRoom);

      if (room.getSensor().getTemperature()>25.0){
        LOGGER.info("Temp (very) too high - set to 0!");
        adjustedForNight = 0;
      } else if (room.getSensor().getTemperature()>24.5){
        LOGGER.info("Temp too high - use only half of calculated!");
        adjustedForNight = .5 * adjustedForNight;
      }

      room.getDigitalOutput().updatePower(adjustedForNight);
    }

    LOGGER.info("Power for room: " + room.getDigitalOutput().getPower());
    roomQueryDocumentSnapshot.getReference().update(Map.of("digitalOutput", room.getDigitalOutput()));
  }

  private static double calculateFromWeatherForecast(WeatherForecast weatherForecast, Room room) {
    double minTemp = room.getTempLower() != null ? room.getTempLower() : -5;
    double maxTemp = room.getTempUpper() != null ? room.getTempUpper() : 10;
    return 1 - ((weatherForecast.getTemp() - minTemp) / (maxTemp - minTemp));
  }

  private static double adjustForNight(double powerForRoom) {
    ZonedDateTime zonedDateTime = getPredictionTimer();
    if (zonedDateTime.getHour() > 20 || zonedDateTime.getHour() < 5) {
      LOGGER.info("Adjusted for night");
      return powerForRoom * 0.25;
    } else {
      return powerForRoom;
    }
  }

  private static WeatherForecast getActualWeatherForecast(
      Building building, ZonedDateTime predictionTimer) throws IOException, InterruptedException {
    List<WeatherForecast> forecast = OpenWeatherMapClient.getForecast(building);
    return forecast.stream()
        .min(Comparator.comparing(o -> absDiff(predictionTimer, o)))
        .orElseThrow(() -> new IllegalStateException("Could not find weather."));
  }

  private static ZonedDateTime getPredictionTimer() {
    return ZonedDateTime.now(ZoneId.of("Europe/Copenhagen")).plusHours(6);
  }

  private static Long absDiff(ZonedDateTime now, WeatherForecast o1) {
    return Math.abs(Duration.between(now, o1.getDateTime()).toSeconds());
  }
}
