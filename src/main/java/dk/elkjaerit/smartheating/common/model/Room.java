package dk.elkjaerit.smartheating.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Room {
  private String id;
  private String name;
  private String sensorId;
  private Double minPower;
  private Double tempLower;
  private Double tempUpper;
  private Boolean night;
  private Sensor sensor;
  private DigitalOutput digitalOutput;
}
