package dk.elkjaerit.smartheating;

import com.google.cloud.bigquery.*;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static dk.elkjaerit.smartheating.ProjectInfo.BIG_QUERY_DATASET_NAME;
import static dk.elkjaerit.smartheating.ProjectInfo.BIG_QUERY_TABLE_NAME;

public class BigQueryRepository {
  private static final Logger logger = Logger.getLogger(BigQueryRepository.class.getName());
  private static final BigQuery BIGQUERY = BigQueryOptions.getDefaultInstance().getService();

  public static void tableInsertRows(Map<String, Object> rowContent) {
    try {
      TableId tableId = TableId.of(BIG_QUERY_DATASET_NAME, BIG_QUERY_TABLE_NAME);

      // Inserts rowContent into datasetName:tableId.
      InsertAllRequest.Builder builder = InsertAllRequest.newBuilder(tableId);

      builder.addRow(rowContent);
      InsertAllRequest build = builder.build();
      InsertAllResponse response = BIGQUERY.insertAll(build);

      if (response.hasErrors()) {
        // If any of the insertions failed, this lets you inspect the errors
        for (Map.Entry<Long, List<BigQueryError>> entry : response.getInsertErrors().entrySet()) {
          logger.info("Response error: \n" + entry.getValue());
        }
      }
    } catch (BigQueryException e) {
      logger.info("Insert operation not performed \n" + e.toString());
    }
  }
}
