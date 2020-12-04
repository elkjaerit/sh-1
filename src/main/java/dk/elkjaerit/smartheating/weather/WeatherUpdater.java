package dk.elkjaerit.smartheating.weather;

import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.protobuf.Timestamp;
import dk.elkjaerit.smartheating.BuildingRepository;
import dk.elkjaerit.smartheating.CloudTask;
import dk.elkjaerit.smartheating.common.model.Building;
import dk.elkjaerit.smartheating.common.model.PredictionOverview;
import dk.elkjaerit.smartheating.common.model.Room;
import dk.elkjaerit.smartheating.ml.Predictor;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WeatherUpdater {
  private static final Logger LOGGER = Logger.getLogger(WeatherUpdater.class.getName());

  public static void main(String[] args) throws IOException {
    run();
  }

  public static void run() throws IOException {
    CloudTask.clearAll();
    BuildingRepository.getBuildings()
        .forEach(
            buildingSnapshot -> {
              updateBuilding(buildingSnapshot);
              try {
                CloudTask.scheduleTask(
                    buildingSnapshot.getId(),
                    Timestamp.newBuilder()
                        .setSeconds(Instant.now().plusSeconds(30).getEpochSecond())
                        .build());
              } catch (IOException e) {
                throw new IllegalStateException(
                    "Could not schedule cloud task after weather update.");
              }
            });
  }

  private static void updateBuilding(QueryDocumentSnapshot buildingSnapshot) {
    LOGGER.info("Update building");
    try {
      Building building = buildingSnapshot.toObject(Building.class);
      WeatherForecast weatherForecast =
          OpenWeatherMapClient.getActualWeatherForecast(building, getPredictionTimer());
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

    PredictionOverview.Label predictedLabel = Predictor.predict(room, weatherForecast);

    if (predictedLabel == PredictionOverview.Label.NEGATIVE) {
      if (room.getMinPower() != null) {
        room.getDigitalOutput().updatePower(room.getMinPower() / 2);
      } else {
        room.getDigitalOutput().updatePower(0);
      }
    } else {
      updateFromWeatherForecast(weatherForecast, room);
    }

    LOGGER.info(
        "Power for '"
            + room.getName()
            + "': "
            + room.getDigitalOutput().getPower()
            + ". Current temp: "
            + (room.getSensor() !=null  ? room.getSensor().getTemperature() : "N/A"));
    roomQueryDocumentSnapshot
        .getReference()
        .update(Map.of("digitalOutput", room.getDigitalOutput()));
  }

  private static void updateFromWeatherForecast(WeatherForecast weatherForecast, Room room) {
    // Calculate from weather and set value
    double power = calculateFromWeatherForecast(weatherForecast, room);
    double minimumPowerForRoom = room.getMinPower() != null ? room.getMinPower() : 0;
    double adjusterForMinimum = Math.max(minimumPowerForRoom, power);

    double tempAdjusted = adjusterForMinimum * room.getTempAdjustFactor();
    double adjustedForNight = adjustForNight(tempAdjusted);

    double halfPowerAsLowest = Math.max(minimumPowerForRoom * 0.5, adjustedForNight);

    room.getDigitalOutput().updatePower(halfPowerAsLowest);
  }

  private static double calculateFromWeatherForecast(WeatherForecast weatherForecast, Room room) {
    double minTemp = room.getTempLower() != null ? room.getTempLower() : -5;
    double maxTemp = room.getTempUpper() != null ? room.getTempUpper() : 10;
    double calculatedValue = 1 - ((weatherForecast.getTemp() - minTemp) / (maxTemp - minTemp));
    return Math.min(1, calculatedValue);
  }

  private static double adjustForNight(double powerForRoom) {
    ZonedDateTime zonedDateTime = getPredictionTimer();
    if (zonedDateTime.getHour() > 22 || zonedDateTime.getHour() < 4) {
      LOGGER.info("Adjusted for night");
      return powerForRoom * 0.5;
    } else {
      return powerForRoom;
    }
  }

  private static ZonedDateTime getPredictionTimer() {
    return ZonedDateTime.now(ZoneId.of("Europe/Copenhagen")).plusHours(6);
  }
}
