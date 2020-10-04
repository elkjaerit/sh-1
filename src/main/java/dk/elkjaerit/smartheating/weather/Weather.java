package dk.elkjaerit.smartheating.weather;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Weather {
    double temp;
    double cloudCover;
    double windSpeed;
    int windDirection;
    double azimuth;
    double zenith;
}
