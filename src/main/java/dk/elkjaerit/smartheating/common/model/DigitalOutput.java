package dk.elkjaerit.smartheating.common.model;

import com.google.cloud.Timestamp;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
public class DigitalOutput {

  private String id;

  private int pinId;

  private Timestamp nextToggleTime;

  private double power;

  private boolean turnedOn;

  private boolean powerOverride;

  public void updatePower(double power) {
    double roundedPower =
        Math.max(0, Math.min(1, ((double) (Math.round(power * 100 / 5))) * 5 / 100));
    if (this.power != roundedPower) {
      this.power = roundedPower;
      nextToggleTime = Timestamp.now();
    }
  }

  /**
   * @param periodInSeconds
   * @return true if state was toggled
   */
  public boolean update(long periodInSeconds) {

    if (power == 0) {
      boolean oldState = turnedOn;
      turnedOn = false;
      nextToggleTime = null;
      return oldState != turnedOn;
    }

    if (power == 1) {
      boolean oldState = turnedOn;
      turnedOn = true;
      nextToggleTime = null;
      return oldState != turnedOn;
    }

    if (nextToggleTime == null || nextToggleTime.compareTo(Timestamp.now()) <= 0) {
      int secondsToAdd = (int) (periodInSeconds * (turnedOn ? (1 - power) : power));
      nextToggleTime =  Timestamp.ofTimeSecondsAndNanos(Instant.now().plusSeconds(secondsToAdd).getEpochSecond(), 0);
      turnedOn = !turnedOn;
      return true;
    }

    return false;
  }
}
