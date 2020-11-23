package dk.elkjaerit.smartheating.ml;

import com.google.cloud.bigquery.*;
import dk.elkjaerit.smartheating.common.model.Room;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static dk.elkjaerit.smartheating.BuildingRepository.getRooms;

public class ModelCreator {
  private static final BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();
  private static final Logger LOG = Logger.getLogger(ModelCreator.class.getName());

  public static void main(String[] args) {
    try {
      create("B93A7D80");
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void create(String gatewayId) throws ExecutionException, InterruptedException {
    getRooms(gatewayId)
            .stream()
            .map(queryDocumentSnapshot -> queryDocumentSnapshot.toObject(Room.class))
           // .filter(room -> room.getName().equals("Ida"))
            .forEach(ModelCreator::buildModelForRoom);
  }

  private static void buildModelForRoom(Room room) {
    LOG.info("Updating : " + room.getName());
    String query = null;
    try {
      query = buildQuery(room);

      QueryJobConfiguration queryConfig =
          QueryJobConfiguration.newBuilder(query)
              .setUseLegacySql(false)
              .build();

      JobId jobId = JobId.of(UUID.randomUUID().toString());

      Job job = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
      job.waitFor();

      //      if (job.getStatus().getError() != null) {
      //        System.out.println(job.getStatus().getError().toString());
      //      }
      LOG.info("Job " + jobId.getJob() + " created for " + room.getName());
    } catch (IOException | InterruptedException | URISyntaxException | BigQueryException e) {
      e.printStackTrace();
    }
  }

  private static String buildQuery(Room room) throws IOException, URISyntaxException {
    String rawQuery =
        Files.readString(
            Path.of(ModelCreator.class.getClassLoader().getResource("create-model.sql").toURI()));
    return String.format(rawQuery, "smart-heating-1.sensors." + room.getName(), room.getId());
  }
}
