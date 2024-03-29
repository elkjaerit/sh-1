package dk.elkjaerit.smartheating.common.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Arrays;
import java.util.List;

@Value
@Builder
public class PredictionOverview {

  Label calculated;
  @Singular List<ResultRow> results;

  @Value
  @Builder
  public static class ResultRow {
    Label label;
    double prohibition;
  }

  public enum Label {
    POSITIVE("1"),
    NEGATIVE("0");

    private final String value;

    Label(String value) {
      this.value = value;
    }

    public static Label create(String valueAsString) {
      return Arrays.stream(values())
          .filter(label -> label.value.equals(valueAsString))
          .findFirst()
          .orElseThrow(
              () -> new IllegalArgumentException("Could not find label from: " + valueAsString));
    }
  }
}
