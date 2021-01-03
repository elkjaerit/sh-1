package dk.elkjaerit.smartheating.sensor;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import dk.elkjaerit.smartheating.BigQueryRepository;
import dk.elkjaerit.smartheating.BuildingRepository;
import dk.elkjaerit.smartheating.common.model.Building;
import dk.elkjaerit.smartheating.common.model.Room;
import dk.elkjaerit.smartheating.common.model.SensorData;
import dk.elkjaerit.smartheating.weather.OpenWeatherMapClient;
import dk.elkjaerit.smartheating.weather.Weather;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class SensorUpdater {

  private static final int UPDATE_INTERVAL_SECONDS = 60;
  private static final Logger logger = Logger.getLogger(SensorUpdater.class.getName());

  public static void updateSensorData(SensorData sensorData)
      throws ExecutionException, InterruptedException {
    Optional<QueryDocumentSnapshot> buildingSnapshot =
        BuildingRepository.getBuildingByGatewayId(sensorData.getDeviceId());

    if (buildingSnapshot.isPresent()) {
      QueryDocumentSnapshot buildingSnap = buildingSnapshot.get();
      updateBuilding(sensorData, buildingSnap);
    } else {

    }
  }

  private static void updateBuilding(SensorData sensorData, QueryDocumentSnapshot buildingSnap)
      throws ExecutionException, InterruptedException {
    Optional<QueryDocumentSnapshot> roomSnap =
        BuildingRepository.getRoomBySensorId(buildingSnap.getReference(), sensorData.getMac());

    if (roomSnap.isPresent()) {
      QueryDocumentSnapshot roomSnapshot = roomSnap.get();
      if (sensorData.isBatteryUpdate()) {
        updateBatteryAndRssi(sensorData, roomSnapshot);
      } else {
        updateTempAndHumidity(sensorData, roomSnapshot);
      }
    } else {
      logger.info("Could not find room for sensordata: " + sensorData);
    }
  }

  private static void updateBatteryAndRssi(
      SensorData sensorData, QueryDocumentSnapshot roomSnapshot) {
    roomSnapshot.getReference().update(Map.of("sensor.battery", sensorData.getBatt()));
  }

  private static void updateTempAndHumidity(
      SensorData sensorData, QueryDocumentSnapshot roomSnapshot)
      throws ExecutionException, InterruptedException {
    if (sensorDataIsOutdated(roomSnapshot)) {
      Building building =
          roomSnapshot.getReference().getParent().getParent().get().get().toObject(Building.class);
      Weather currentWeather = OpenWeatherMapClient.getCurrentWeather(building);
      Map<String, Object> rowContent =
          createRowFromJson(sensorData, currentWeather, roomSnapshot.toObject(Room.class));
      BigQueryRepository.tableInsertRows(rowContent);
      updateRoomSensorData(roomSnapshot, sensorData);
    }
  }

  private static boolean sensorDataIsOutdated(QueryDocumentSnapshot roomSnap) {
    Timestamp timestamp = roomSnap.getTimestamp("sensor.lastUpdated");
    return timestamp == null
        || Timestamp.now().getSeconds() - timestamp.getSeconds() > UPDATE_INTERVAL_SECONDS;
  }

  private static void updateRoomSensorData(QueryDocumentSnapshot room, SensorData sensorData) {
    Map<String, Object> values =
        Map.of(
            "sensor.lastUpdated",
            Timestamp.now(),
            "sensor.temperature",
            sensorData.getTemp(),
            "sensor.rssi",
            sensorData.getRssi(),
            "sensor.humidity",
            sensorData.getHumidity());
    room.getReference().update(values);
  }

  private static Map<String, Object> createRowFromJson(
      SensorData sensorData, Weather currentWeaher, Room room) {
    Map<String, Object> rowContent = new HashMap<>();
    rowContent.put("created", sensorData.getTimestamp());
    rowContent.put("gatewayId", sensorData.getDeviceId());
    rowContent.put("sensorId", sensorData.getMac());
    rowContent.put("roomId", room.getId());
    rowContent.put("temperature", sensorData.getTemp());
    rowContent.put("out_temp", currentWeaher.getTemp());
    rowContent.put("clouds", currentWeaher.getCloudCover());
    rowContent.put("wind_speed", currentWeaher.getWindSpeed());
    rowContent.put("wind_direction", currentWeaher.getWindDirection());
    rowContent.put("humidity", sensorData.getHumidity());
    rowContent.put("rssi", sensorData.getRssi());
    if (room.getDigitalOutput() != null) {
      rowContent.put("power", room.getDigitalOutput().getPower());
    }
    rowContent.put(
        "azimuth", new BigDecimal(currentWeaher.getAzimuth()).setScale(2, RoundingMode.HALF_UP));
    rowContent.put(
        "zenith", new BigDecimal(currentWeaher.getZenith()).setScale(2, RoundingMode.HALF_UP));
    return rowContent;
  }
}
