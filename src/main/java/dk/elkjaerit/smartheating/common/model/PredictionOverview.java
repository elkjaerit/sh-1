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

  public Label getResult() {
    if (calculated == Label.POSITIVE) {
      return Label.POSITIVE;
    } else {
      double negativeProhibition = results.stream()
              .filter(resultRow -> resultRow.label == Label.NEGATIVE)
              .map(resultRow -> resultRow.prohibition)
              .findFirst()
              .orElseThrow(IllegalArgumentException::new);
      if (negativeProhibition > 0.55) {
        return Label.NEGATIVE;
      }
    }
    return Label.POSITIVE;
  }

  @Value
  @Builder
  public static class ResultRow {
    Label label;
    double prohibition;
  }

  public enum Label {
    POSITIVE("NONE"),
    NEGATIVE("HIGH_INC");

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
