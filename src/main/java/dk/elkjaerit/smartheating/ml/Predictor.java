package dk.elkjaerit.smartheating.ml;

import com.google.cloud.bigquery.*;
import dk.elkjaerit.smartheating.BuildingRepository;
import dk.elkjaerit.smartheating.model.Building;
import dk.elkjaerit.smartheating.model.PredictionOverview;
import dk.elkjaerit.smartheating.model.PredictionOverview.Label;
import dk.elkjaerit.smartheating.model.Room;
import dk.elkjaerit.smartheating.weather.WeatherForecast;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Predictor {
  private static final Logger LOG = Logger.getLogger(Predictor.class.getName());
  private static final BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

  public static void main(String[] args) {
    Building building =
        BuildingRepository.getBuildingByGatewayId("B93A7D80").get().toObject(Building.class);

    building.getRooms().stream()
        .filter(room -> room.getName().equals("Alrum"))
        .forEach(
            room -> {
              Label res =
                  predict(
                      room,
                      WeatherForecast.builder().cloudCover(30).azimuth(180).zenith(65).build());
              System.out.println(room.getName() + ": " + res);
            });
  }

  public static Label predict(Room room, WeatherForecast weatherForecast) {
    try {
      String query = buildQuery(room, weatherForecast);
      QueryJobConfiguration queryConfig =
          QueryJobConfiguration.newBuilder(query)
              .setUseLegacySql(false)
              .build();
      JobId jobId = JobId.of(UUID.randomUUID().toString());

      Job job = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
      job.waitFor();

      PredictionOverview build = createResult(job);
      LOG.info("" + build);
      return build.getResult();
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Could not make prediction for room " + room.getName(), e);
      return Label.NEGATIVE;
    }
  }

  private static PredictionOverview createResult(Job job) throws InterruptedException {
    TableResult result = job.getQueryResults();

    List<FieldValueList> rows = new ArrayList<>();
    result.iterateAll().forEach(rows::add);

    FieldValueList row = rows.get(0);

    FieldValueList positive = row.get(1).getRecordValue().get(0).getRecordValue();
    FieldValueList negative = row.get(1).getRecordValue().get(1).getRecordValue();

    return PredictionOverview.builder()
        .calculated(Label.create(row.get(0).getStringValue()))
        .result(
            PredictionOverview.ResultRow.builder()
                .label(Label.POSITIVE)
                .prohibition(positive.get(1).getDoubleValue())
                .build())
        .result(
            PredictionOverview.ResultRow.builder()
                .label(Label.NEGATIVE)
                .prohibition(negative.get(1).getDoubleValue())
                .build())
        .build();
  }

  private static String buildQuery(Room room, WeatherForecast weatherForecast)
      throws IOException, URISyntaxException {
    String fileContent =
        IOUtils.toString(
            Predictor.class.getClassLoader().getResourceAsStream("predict.sql"),
            StandardCharsets.UTF_8);
    return String.format(
        fileContent,
        "smart-heating-1.sensors." + room.getName(),
        (int) weatherForecast.getCloudCover(),
        (int) weatherForecast.getAzimuth(),
        (int) weatherForecast.getZenith());
  }
}
