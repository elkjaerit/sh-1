package dk.elkjaerit.smartheating.ml;

import com.google.cloud.bigquery.*;
import dk.elkjaerit.smartheating.BuildingRepository;
import dk.elkjaerit.smartheating.model.Building;
import dk.elkjaerit.smartheating.model.Room;
import dk.elkjaerit.smartheating.weather.WeatherForecast;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Predictor {
  private static final BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

  public static void main(String[] args) {
    Building building =
        BuildingRepository.getBuildingByGatewayId("B93A7D80").get().toObject(Building.class);

    building.getRooms().stream()
        .filter(room -> room.getName().equals("Alrum"))
        .forEach(
            room ->
                predict(
                    room,
                    WeatherForecast.builder().cloudCover(30).azimuth(180).zenith(65).build()));
  }

  public static boolean predict(Room room, WeatherForecast weatherForecast) {
    String query = null;
    try {
      query = buildQuery(room, weatherForecast);

      QueryJobConfiguration queryConfig =
          QueryJobConfiguration.newBuilder(query)
              // Use standard SQL syntax for queries.
              // See: https://cloud.google.com/bigquery/sql-reference/
              .setUseLegacySql(false)
              .build();

      JobId jobId = JobId.of(UUID.randomUUID().toString());

      Job job = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
      job.waitFor();

      TableResult result = job.getQueryResults();

      List<FieldValueList> rows = new ArrayList<>();
      result.iterateAll().forEach(rows::add);
      FieldValueList row = (FieldValueList) rows.get(0);
      String prediction = row.get(0).getStringValue();

      String predic = row.get(1).getRecordValue().get(0).getRecordValue().get(0).getStringValue();
      double prohi = row.get(1).getRecordValue().get(0).getRecordValue().get(1).getDoubleValue();

      String predic1 = row.get(1).getRecordValue().get(1).getRecordValue().get(0).getStringValue();
      double prohi1 = row.get(1).getRecordValue().get(1).getRecordValue().get(1).getDoubleValue();

      double threshold = 0.65;
      return prohi1 > threshold;

      //        result
      //          .iterateAll()
      //          .forEach(
      //              row -> {
      //                String prediction = row.get(0).getStringValue();
      //
      //                String predic =
      //
      // row.get(1).getRecordValue().get(0).getRecordValue().get(0).getStringValue();
      //                double prohi =
      //
      // row.get(1).getRecordValue().get(0).getRecordValue().get(1).getDoubleValue();
      //
      //                String predic1 =
      //
      // row.get(1).getRecordValue().get(1).getRecordValue().get(0).getStringValue();
      //                double prohi1 =
      //
      // row.get(1).getRecordValue().get(1).getRecordValue().get(1).getDoubleValue();
      //
      //                if (prohi1 > 0.65) {
      //                  return true;
      //                }

      //                System.out.println(predic + ": " + prohi);
      //                System.out.println(predic1 + ": " + prohi1);
      //
      //                row.forEach(fieldValue -> System.out.println(fieldValue.toString()
      // + ", "));
      //                System.out.println();
      //              });

    } catch (IOException | URISyntaxException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return false;
  }

  private static String buildQuery(Room room, WeatherForecast weatherForecast)
      throws IOException, URISyntaxException {
    String rawQuery =
        Files.readString(
            Path.of(ModelCreator.class.getClassLoader().getResource("predict.sql").toURI()));
    return String.format(
        rawQuery,
        "smart-heating-1.sensors." + room.getName(),
        (int) weatherForecast.getCloudCover(),
        (int) weatherForecast.getAzimuth(),
        (int) weatherForecast.getZenith());
  }
}
