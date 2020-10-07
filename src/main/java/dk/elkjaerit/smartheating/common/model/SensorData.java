package dk.elkjaerit.smartheating.common.model;

import lombok.Builder;
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
