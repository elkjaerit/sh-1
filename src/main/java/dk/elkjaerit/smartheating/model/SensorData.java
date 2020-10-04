package dk.elkjaerit.smartheating.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@Builder
public class SensorData {
    long timestamp;
    String deviceId;
    String mac;
    double temp;
    double humidity;
    double batt;
    int rssi;
}
