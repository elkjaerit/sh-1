package dk.elkjaerit.smartheating.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MachineLearning {
    Double threshold;
    Double accuracy;
    Double recall;
}
