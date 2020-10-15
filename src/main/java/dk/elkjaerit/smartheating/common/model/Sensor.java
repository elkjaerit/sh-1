package dk.elkjaerit.smartheating.common.model;

import com.google.cloud.Timestamp;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {
    Timestamp lastUpdated;
    Double temperature;
    Double humidity;
    Integer rssi;
    Double battery;
}
