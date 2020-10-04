package dk.elkjaerit.smartheating.model;

import com.google.cloud.Timestamp;
import lombok.*;

import java.time.LocalDateTime;

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
