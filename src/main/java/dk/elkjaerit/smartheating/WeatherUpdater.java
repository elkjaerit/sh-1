package dk.elkjaerit.smartheating;

import dk.elkjaerit.smartheating.model.Building;
import dk.elkjaerit.smartheating.model.DigitalOutput;
import dk.elkjaerit.smartheating.model.Room;
import dk.elkjaerit.smartheating.weather.OpenWeatherMapClient;
import dk.elkjaerit.smartheating.weather.WeatherForecast;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public class WeatherUpdater {
  private static final Logger LOGGER = Logger.getLogger(WeatherUpdater.class.getName());

  public static void main(String[] args) {
    run();
  }

  public static void run() {
    BuildingRepository.getBuildings()
        .forEach(
            queryDocumentSnapshot -> {
              Building building = queryDocumentSnapshot.toObject(Building.class);
              updateBuilding(building);
              queryDocumentSnapshot.getReference().set(building);
            });
  }

  private static void updateBuilding(Building building) {
    LOGGER.info("Update building");
    try {
      WeatherForecast weatherForecast = getActualWeatherForecast(building);
      LOGGER.info("Weather forecast: " + weatherForecast);
      building.getRooms().forEach(room -> updateRoom(room, weatherForecast));
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static void updateRoom(Room room, WeatherForecast weatherForecast) {
    LOGGER.info("Updating room : " + room.getName());

    double minTemp = room.getTempLower() != null ? room.getTempLower() : -5;
    double maxTemp = room.getTempUpper() != null ? room.getTempUpper() : 15;

    double powerCalculatedFromWeather =
        1 - ((weatherForecast.getTemp() - minTemp) / (maxTemp - minTemp));

    double minimumPowerForRoom = room.getMinPower() != null ? room.getMinPower() : 0;
    double powerForRoom = Math.max(minimumPowerForRoom, Math.min(1, powerCalculatedFromWeather));

    double adjustedForNight = adjustForNight(powerForRoom);

    if (room.getDigitalOutput() == null) {
      room.setDigitalOutput(DigitalOutput.builder().build());
    }

    room.getDigitalOutput().updatePower(adjustedForNight);

    LOGGER.info("Power for room: " + room.getDigitalOutput().getPower());
  }

  private static double adjustForNight(double powerForRoom) {
    ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen")).plusHours(6);
    if (zonedDateTime.getHour() > 20 || zonedDateTime.getHour() < 5) {
      LOGGER.info("Adjusted for night");
      return powerForRoom * 0.25;
    } else {
      return powerForRoom;
    }
  }

  private static WeatherForecast getActualWeatherForecast(Building building)
      throws IOException, InterruptedException {
    List<WeatherForecast> forecast = OpenWeatherMapClient.getForecast(building);
    ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen")).plusHours(6);
    return forecast.stream()
        .min(Comparator.comparing(o -> absDiff(now, o)))
        .orElseThrow(() -> new IllegalStateException("Could not find weather."));
  }

  private static Long absDiff(ZonedDateTime now, WeatherForecast o1) {
    return Math.abs(Duration.between(now, o1.getDateTime()).toSeconds());
  }
}