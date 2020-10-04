package dk.elkjaerit.smartheating.ml;

import com.google.cloud.bigquery.*;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import dk.elkjaerit.smartheating.BuildingRepository;
import dk.elkjaerit.smartheating.model.Building;
import dk.elkjaerit.smartheating.model.Room;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class ModelCreator {
  private static final BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();
  private static final Logger LOG = Logger.getLogger(ModelCreator.class.getName());

  public static void main(String[] args) {
    Optional<QueryDocumentSnapshot> building =
        BuildingRepository.getBuildingByGatewayId("B93A7D80");
    Building building1 = building.get().toObject(Building.class);
    create(building1);
  }

  public static void create(Building building) {
    building.getRooms().stream()
        .filter(room -> room.getName().equals("Alrum"))
        .forEach(ModelCreator::buildModelForRoom);
  }

  private static void buildModelForRoom(Room room) {
    LOG.info("Updating : " + room.getName());
    String query = null;
    try {
      query = buildQuery(room);

      QueryJobConfiguration queryConfig =
          QueryJobConfiguration.newBuilder(query)
              // Use standard SQL syntax for queries.
              // See: https://cloud.google.com/bigquery/sql-reference/
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
