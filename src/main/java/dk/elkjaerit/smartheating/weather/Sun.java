package dk.elkjaerit.smartheating.weather;

import net.e175.klaus.solarpositioning.AzimuthZenithAngle;
import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;

public class Sun {

  public static AzimuthZenithAngle getAzimuthAndZenithAngle(double lat, double lon, ZonedDateTime zonedDateTime) {
    final GregorianCalendar dateTime = GregorianCalendar.from(zonedDateTime);
    return SPA.calculateSolarPosition(
        dateTime,
        lat,
        lon,
        1, // elevation (m)
        DeltaT.estimate(dateTime), // delta T (s)
        1010, // avg. air pressure (hPa)
        11); // avg. air temperature (°C)
  }

  public static void main(String[] args) {
    System.out.println(getAzimuthAndZenithAngle(56.3, 10.3, ZonedDateTime.now(ZoneId.of("UTC"))));
  }
}
