package dk.elkjaerit.smartheating.ml;

import com.google.cloud.bigquery.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class ConfusionMatrix {
  private static final Logger LOG = Logger.getLogger(ConfusionMatrix.class.getName());
  private static final BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

  public static void main(String[] args) {
    try {

      double correct = 0;
      double threshold = 0.5;

      do {
        String query = buildQuery(null, threshold);
        Job job = executeQuery(query);
        correct = createResultFromJob(job);
        threshold += 0.02;
      } while (correct < 0.85 && threshold <= 1);

      System.out.println("threshold: " + threshold + ", correct: " + correct);

    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
    }
  }

  private static Job executeQuery(String query) throws InterruptedException {
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(query).setUseLegacySql(false).build();
    JobId jobId = JobId.of(UUID.randomUUID().toString());

    Job job = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
    job.waitFor();
    return job;
  }

  private static double createResultFromJob(Job job) throws InterruptedException {
    TableResult result = job.getQueryResults();
    var highIncIndex = result.getSchema().getFields().getIndex("HIGH_INC");
    var noneIndex = result.getSchema().getFields().getIndex("NONE");

    List<FieldValueList> rows = new ArrayList<>();
    result.iterateAll().forEach(rows::add);
    FieldValueList high_inc =
        rows.stream()
            .filter(fieldValues -> fieldValues.get(0).getStringValue().equals("HIGH_INC"))
            .findFirst()
            .orElseThrow();

    var high = high_inc.get(highIncIndex).getDoubleValue();
    var none = high_inc.get(noneIndex).getDoubleValue();
    var total = high + none;

    return high / total;
  }

  private static String buildQuery(String roomId, double threshold) throws IOException {
    String fileContent =
        IOUtils.toString(
            Predictor.class.getClassLoader().getResourceAsStream("confusion_matrix.sql"),
            StandardCharsets.UTF_8);
    return String.format(fileContent, threshold);
  }
}
