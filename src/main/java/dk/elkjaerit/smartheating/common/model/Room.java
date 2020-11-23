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
  private Double preferredTemp;
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
    if (preferredTemp == null) {
      return 1;
    }
    double tempDiff = sensor.getTemperature() - preferredTemp;

    if (tempDiff > 2) {
      if (minPower != null) {
        return 0.25;
      } else {
        return 0;
      }
    } else if (tempDiff > 1) {
      if (minPower != null) {
        return 0.5;
      } else {
        return 0.25;
      }
    } else {
      return 1;
    }
  }
}
