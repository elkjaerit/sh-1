package dk.elkjaerit.smartheating.functions;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.gson.Gson;
import dk.elkjaerit.smartheating.BigQueryRepository;
import dk.elkjaerit.smartheating.BuildingRepository;
import dk.elkjaerit.smartheating.common.model.Building;
import dk.elkjaerit.smartheating.common.model.Room;
import dk.elkjaerit.smartheating.common.model.Sensor;
import dk.elkjaerit.smartheating.common.model.SensorData;
import dk.elkjaerit.smartheating.weather.OpenWeatherMapClient;
import dk.elkjaerit.smartheating.weather.Weather;
import lombok.AllArgsConstructor;
import lombok.Value;

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
  public static final int UPDATE_INTERVAL_SECONDS = 60;

  @Override
  public void accept(PubSubMessage message, Context context) {
    String data = new String(Base64.getDecoder().decode(message.data));
    logger.info("Received data: " + data);

    SensorData sensorData = gson.fromJson(data, SensorData.class);

    Optional<QueryDocumentSnapshot> buildingSnapshot =
        BuildingRepository.getBuildingByGatewayId(sensorData.getDeviceId());

    if (buildingSnapshot.isPresent()) {
      QueryDocumentSnapshot buildingSnap = buildingSnapshot.get();

      Optional<QueryDocumentSnapshot> roomSnap =
          BuildingRepository.getRoomBySensorId(buildingSnap.getReference(), sensorData.getMac());

      Optional<Room> room =
          roomSnap.map(queryDocumentSnapshot -> queryDocumentSnapshot.toObject(Room.class));

      if (room.isPresent()) {
        if (sensorDataIsOutdated(roomSnap.get())) {
          Building building = buildingSnap.toObject(Building.class);
          Weather currentWeather = OpenWeatherMapClient.getCurrentWeather(building);
          Map<String, Object> rowContent = createRowFromJson(sensorData, currentWeather, room);
          BigQueryRepository.tableInsertRows(rowContent);
          updateRoomSensorData(roomSnap.get(), sensorData);
        }
      }
    }
  }

  private boolean sensorDataIsOutdated(QueryDocumentSnapshot roomSnap) {
    Timestamp timestamp = roomSnap.getTimestamp("sensor.lastUpdated");
    return timestamp != null
            && Timestamp.now().getSeconds() - timestamp.getSeconds() > UPDATE_INTERVAL_SECONDS;
  }

  private void updateRoomSensorData(QueryDocumentSnapshot room, SensorData sensorData) {
    room.getReference()
        .update(
            Map.of(
                "sensor",
                Sensor.builder()
                    .lastUpdated(Timestamp.now())
                    .temperature(sensorData.getTemp())
                    .rssi(sensorData.getRssi())
                    .humidity(sensorData.getHumidity())
                    .build()));
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

  @Value
  @AllArgsConstructor
  public static class PubSubMessage {
    String data;
    Map<String, String> attributes;
    String messageId;
    String publishTime;
  }
}
