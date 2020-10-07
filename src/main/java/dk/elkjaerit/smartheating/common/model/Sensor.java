package dk.elkjaerit.smartheating.common.model;

import com.google.cloud.Timestamp;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {
    double temperature;
    Timestamp lastUpdated;
    double humidity;
    int rssi;
}
