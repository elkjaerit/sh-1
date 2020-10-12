package dk.elkjaerit.smartheating.functions;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.gson.Gson;
import dk.elkjaerit.smartheating.common.model.SensorData;
import dk.elkjaerit.smartheating.sensor.SensorUpdater;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Consumer implements BackgroundFunction<Consumer.PubSubMessage> {
  private static final Logger logger = Logger.getLogger(Consumer.class.getName());
  private static final Gson gson = new Gson();

  @Override
  public void accept(PubSubMessage message, Context context) {
    String data = new String(Base64.getDecoder().decode(message.data));
    logger.info("Received data: " + data);
    SensorData sensorData = gson.fromJson(data, SensorData.class);
    try {
      SensorUpdater.updateSensorData(sensorData);
    } catch (ExecutionException | InterruptedException e) {
      logger.log(Level.SEVERE, "Could not update" + sensorData, e);
    }
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
