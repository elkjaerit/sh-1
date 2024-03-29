package dk.elkjaerit.smartheating.common.model;

import com.google.cloud.firestore.GeoPoint;
import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Building {
    @Singular
    private List<String> gatewayIds;
    private GeoPoint location;
    private String cityId;
}
