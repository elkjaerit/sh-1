package dk.elkjaerit.smartheating.ml;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import java.util.UUID;


public class SimpleApp {
  public static void main(String... args) throws Exception {
    BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(
                "SELECT\n"
                    + "  *\n"
                    + "FROM\n"
                    + "  ML.PREDICT (MODEL `smart-heating-1.sensors.5e2db47e_474c_4591_90ca_ba8d033612d1`,\n"
                    + "    (\n"
                    + "    SELECT\n"
                    + "      100 as cloud_bucket, 120 as azimuth_bucket, 65 as zenith_bucket\n"
                    + "    )\n"
                    + "  )")
            // Use standard SQL syntax for queries.
            // See: https://cloud.google.com/bigquery/sql-reference/
            .setUseLegacySql(false)
            .build();

    // Create a job ID so that we can safely retry.
    JobId jobId = JobId.of(UUID.randomUUID().toString());
    Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

    // Wait for the query to complete.
    queryJob = queryJob.waitFor();

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
      String url = row.get("predicted_label" +
              "").getStringValue();
      // long viewCount = row.get("view_count").getLongValue();
      System.out.println(url);
    }
  }
}
