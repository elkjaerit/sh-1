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

  public DigitalOutput getDigitalOutput() {
    if (this.digitalOutput == null) {
      this.digitalOutput = DigitalOutput.builder().build();
    }
    return this.digitalOutput;
  }

  public double getTempAdjustFactor() {
    double preferred = 24d;

    if (sensor.getTemperature() - preferred > 1) {
      if (minPower != null) {
        return 0.5;
      } else {
        return 0;
      }
    } else if (sensor.getTemperature() - preferred > 0.5) {
      if (minPower != null) {
        return 1;
      } else {
        return 0.5;
      }
    } else {
      return 1;
    }
  }
}
