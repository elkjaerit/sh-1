package dk.elkjaerit.smartheating.weather;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Value
@Builder
public class WeatherForecast {
    ZonedDateTime dateTime;
    double temp;
    double cloudCover;
    double windSpeed;
    int windDirection;
    double azimuth;
    double zenith;
}
