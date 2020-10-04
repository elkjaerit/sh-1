package dk.elkjaerit.smartheating.ml;

import com.google.cloud.bigquery.*;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

public class MlCreator {

  @SneakyThrows
  public static void createModel() {
    BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();
    String rawQuery =
        Files.readString(
            Path.of(ModelCreator.class.getClassLoader().getResource("create-model.sql").toURI()));
    var query =
        String.format(
            rawQuery, "smart-heating-1.sensors.modelstue", "ca306115-0050-472c-b2dc-652247bf5342");
    System.out.println(query);
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(query)
            // Use standard SQL syntax for queries.
            // See: https://cloud.google.com/bigquery/sql-reference/
            .setUseLegacySql(false)
            .build();

    // Create a job ID so that we can safely retry.
    JobId jobId = JobId.of(UUID.randomUUID().toString());
    System.out.println("Job started: " + LocalDateTime.now());
    Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
    // Wait for the query to complete.
    queryJob = queryJob.waitFor();
    System.out.println("Job ended: " + LocalDateTime.now());

    // Check for errors
    if (queryJob == null) {
      throw new RuntimeException("Job no longer exists");
    } else if (queryJob.getStatus().getError() != null) {
      // You can also look at queryJob.getStatus().getExecutionErrors() for all
      // errors, not just the latest one.
      throw new RuntimeException(queryJob.getStatus().getError().toString());
    }

    // Get the results.
    TableResult result = queryJob.getQueryResults();

    // Print all pages of the results.
    for (FieldValueList row : result.iterateAll()) {
      String url = row.get("url").getStringValue();
      long viewCount = row.get("view_count").getLongValue();
      System.out.printf("url: %s views: %d%n", url, viewCount);
    }
  }
}
