package dk.elkjaerit.smartheating.common.model;

import lombok.Builder;
import lombok.Value;

import java.util.Objects;

@Value
@Builder
public class SensorData {
    Long timestamp;
    String deviceId;
    String mac;
    Double temp;
    Double humidity;
    Double batt;
    Integer rssi;

    public boolean isBatteryUpdate(){
        return Objects.nonNull(batt);
    }
}
