package dk.elkjaerit.smartheating.functions;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.gson.Gson;
import dk.elkjaerit.smartheating.BuildingRepository;
import dk.elkjaerit.smartheating.BigQueryRepository;
import dk.elkjaerit.smartheating.common.model.Building;
import dk.elkjaerit.smartheating.common.model.Room;
import dk.elkjaerit.smartheating.common.model.Sensor;
import dk.elkjaerit.smartheating.common.model.SensorData;
import dk.elkjaerit.smartheating.weather.OpenWeatherMapClient;
import dk.elkjaerit.smartheating.weather.Weather;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class Consumer implements BackgroundFunction<Consumer.PubSubMessage> {
  private static final Logger logger = Logger.getLogger(Consumer.class.getName());
  private static final Gson gson = new Gson();

  @Override
  public void accept(PubSubMessage message, Context context) {
    String data = new String(Base64.getDecoder().decode(message.data));
    logger.info("Received data: " + data);
    SensorData sensorData = gson.fromJson(data, SensorData.class);

    Optional<QueryDocumentSnapshot> buildingRef =
        BuildingRepository.getBuildingByGatewayId(sensorData.getDeviceId());

    if (buildingRef.isPresent()) {

      Building building = buildingRef.get().toObject(Building.class);
      Optional<Room> room = findRoom(building, sensorData);

      Weather currentWeather = OpenWeatherMapClient.getCurrentWeather(building);
      Map<String, Object> rowContent = createRowFromJson(sensorData, currentWeather, room);
      BigQueryRepository.tableInsertRows(rowContent);

      logger.info("Data inserted: " + room.map(Room::getName).orElse("-"));
      updateBuilding(sensorData, buildingRef, building, room);
    }
  }

  private void updateBuilding(SensorData sensorData, Optional<QueryDocumentSnapshot> buildingRef, Building building, Optional<Room> room) {
    if (room.isPresent()) {
      room.get()
          .setSensor(
              Sensor.builder()
                  .lastUpdated(Timestamp.now())
                  .temperature(sensorData.getTemp())
                  .rssi(sensorData.getRssi())
                  .humidity(sensorData.getHumidity())
                  .build());
      buildingRef.get().getReference().set(building);
    }
  }

  private Map<String, Object> createRowFromJson(
      SensorData sensorData, Weather currentWeaher, Optional<Room> room) {
    Map<String, Object> rowContent = new HashMap<>();
    rowContent.put("created", sensorData.getTimestamp());
    rowContent.put("gatewayId", sensorData.getDeviceId());
    rowContent.put("sensorId", sensorData.getMac());
    rowContent.put("roomId", room.map(Room::getId).orElse(null));
    rowContent.put("temperature", sensorData.getTemp());
    rowContent.put("out_temp", currentWeaher.getTemp());
    rowContent.put("clouds", currentWeaher.getCloudCover());
    rowContent.put("wind_speed", currentWeaher.getWindSpeed());
    rowContent.put("wind_direction", currentWeaher.getWindDirection());
    rowContent.put("humidity", sensorData.getHumidity());
    rowContent.put("rssi", sensorData.getRssi());
    rowContent.put(
        "azimuth", new BigDecimal(currentWeaher.getAzimuth()).setScale(2, RoundingMode.HALF_UP));
    rowContent.put(
        "zenith", new BigDecimal(currentWeaher.getZenith()).setScale(2, RoundingMode.HALF_UP));
    return rowContent;
  }

  private Optional<Room> findRoom(Building building, SensorData requestJson) {
    return building.getRooms().stream()
        .filter(room -> StringUtils.equals(room.getSensorId(), requestJson.getMac()))
        .findFirst();
  }

  public static class PubSubMessage {
    String data;
    Map<String, String> attributes;
    String messageId;
    String publishTime;
  }
}
