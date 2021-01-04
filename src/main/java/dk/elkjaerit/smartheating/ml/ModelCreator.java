package dk.elkjaerit.smartheating.ml;

import com.google.cloud.bigquery.*;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.SetOptions;
import dk.elkjaerit.smartheating.common.model.Room;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
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
    getRooms(gatewayId).stream()
        //.map(queryDocumentSnapshot -> queryDocumentSnapshot.toObject(Room.class))
//         .filter(room -> room.getName().equals("Alrum"))
        .forEach(ModelCreator::buildModelForRoom);
  }

  private static void buildModelForRoom(QueryDocumentSnapshot roomSnapshot) {

    Room room = roomSnapshot.toObject(Room.class);
//    if (!room.getName().equals("Kontor")) return;
    LOG.info("Updating : " + room.getName());
    try {
      buildModel(room);
      var job = executeQuery(buildRocEvaluationQuery(room));
      TableResult result = job.getQueryResults();

      List<FieldValueList> rows = new ArrayList<>();
      result.iterateAll().forEach(rows::add);
      var threshold = rows.get(0).get("threshold").getDoubleValue();
      var fromRecall = rows.get(0).get("from_desired_recall").getDoubleValue();
      var falsePositiveRate = rows.get(0).get("false_positive_rate").getDoubleValue();

      LOG.info(room.getName() + ": threshold: " + threshold + ", fromRecall: " + fromRecall + ", false_positive_rate: " + falsePositiveRate);

      var evaluationJob = executeQuery(buildEvaluationQuery(room, threshold));
      List<FieldValueList>  evalRows = new ArrayList<>();
      evaluationJob.getQueryResults().iterateAll().forEach(evalRows::add);
      double recall = evalRows.get(0).get("recall").getDoubleValue();
      double accuracy = evalRows.get(0).get("accuracy").getDoubleValue();
      LOG.info(room.getName() + ": Recall: "+ recall + ", accuracy: " + accuracy);

      if (recall > 0.4 && accuracy > 0.5){
        LOG.info("Found good model for: " + room.getName());
        Map<String, Double> mlValues = Map.of("threshold", threshold, "recall", recall, "accuracy", accuracy);
        roomSnapshot.getReference().set(Map.of("ml", mlValues),SetOptions.merge());
      }




      if (fromRecall < 0.2) {
        LOG.info("Model created - use threshold: " + threshold);
      }
    } catch (IOException | InterruptedException | URISyntaxException | BigQueryException e) {
      LOG.log(Level.SEVERE, "Error creating ML Model for " + room.getName());
    }
  }

  private static void buildModel(Room room)
      throws IOException, URISyntaxException, InterruptedException {
    var query = buildQuery(room);
    executeQuery(query);
  }

  private static Job executeQuery(String query) throws InterruptedException {
    var queryConfig = QueryJobConfiguration.newBuilder(query).setUseLegacySql(false).build();

    JobId jobId = JobId.of(UUID.randomUUID().toString());

    Job job = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
    job.waitFor();
    return job;
  }

  private static String buildQuery(Room room) throws IOException, URISyntaxException {
    String rawQuery =
        Files.readString(
            Path.of(ModelCreator.class.getClassLoader().getResource("create-model.sql").toURI()));
    return String.format(rawQuery, "smart-heating-1.sensors." + room.getName(), room.getId());
  }

  private static String buildRocEvaluationQuery(Room room) throws IOException, URISyntaxException {
    String rawQuery =
        Files.readString(
            Path.of(ModelCreator.class.getClassLoader().getResource("roc-evaluation.sql").toURI()));
    return String.format(rawQuery, "smart-heating-1.sensors." + room.getName(), room.getId());
  }


  private static String buildEvaluationQuery(Room room, double threshold) throws IOException, URISyntaxException {
    String rawQuery =
            Files.readString(
                    Path.of(ModelCreator.class.getClassLoader().getResource("evaluation.sql").toURI()));
    return String.format(rawQuery, "smart-heating-1.sensors." + room.getName(), room.getId(), threshold);
  }

}
