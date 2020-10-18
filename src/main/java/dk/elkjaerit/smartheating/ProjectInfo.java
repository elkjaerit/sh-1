package dk.elkjaerit.smartheating;

public interface ProjectInfo {
  String PROJECT_ID = "smart-heating-1";
  String CLOUD_REGION = "europe-west1";
  String REGISTRY_NAME = "power-unit";

  String BIG_QUERY_DATASET_NAME = "sensors";
  String BIG_QUERY_TABLE_NAME = "sensor";

  String QUEUE_ID = "power-unit-queue";
}
