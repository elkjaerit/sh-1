package dk.elkjaerit.smartheating;

import com.google.cloud.tasks.v2.*;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class CloudTask {
  private static final Logger logger = Logger.getLogger(CloudTask.class.getName());
  private static final Gson gson = new Gson();

  public static void scheduleTask(String buildingId, Timestamp scheduledTime) throws IOException {
    String projectId = ProjectInfo.PROJECT_ID;
    String locationId = ProjectInfo.CLOUD_REGION;
    String queueId = ProjectInfo.QUEUE_ID;
    createTask(projectId, locationId, queueId, buildingId, scheduledTime);
  }

  public static void main(String[] args) throws IOException {
    List<Task> tasks = listTask();
  }

  private static List<Task> listTask() throws IOException {
    try (CloudTasksClient client = CloudTasksClient.create()) {
      QueueName queueName =
          QueueName.newBuilder()
              .setLocation(ProjectInfo.CLOUD_REGION)
              .setProject(ProjectInfo.PROJECT_ID)
              .setQueue(ProjectInfo.QUEUE_ID)
              .build();

      CloudTasksClient.ListTasksPagedResponse listTasksPagedResponse = client.listTasks(queueName);
      return listTasksPagedResponse.getPage().getResponse().getTasksList();
    }
  }

  // Create a task with a HTTP target using the Cloud Tasks client.
  public static void createTask(
      String projectId,
      String locationId,
      String queueId,
      String buildingId,
      Timestamp scheduledTime)
      throws IOException {

    // Instantiates a client.
    try (CloudTasksClient client = CloudTasksClient.create()) {
      String url = "https://europe-west1-smart-heating-1.cloudfunctions.net/power-unit-trigger";

      String body = gson.toJson(Map.of("buildingId", buildingId));

      // Construct the fully qualified queue name.
      String queuePath = QueueName.of(projectId, locationId, queueId).toString();

      // Construct the task body.
      Task taskDefinition =
          Task.newBuilder()
              .setScheduleTime(scheduledTime)
              .setHttpRequest(
                  HttpRequest.newBuilder()
                      .setBody(ByteString.copyFrom(body, Charset.defaultCharset()))
                      .setUrl(url)
                      .setHttpMethod(HttpMethod.POST)
                      .build())
              .build();

      // Send create task request.
      Task task = client.createTask(queuePath, taskDefinition);
      logger.info("Task created: " + task.getName());
    }
  }

  public static void clearAll() throws IOException {
    try (CloudTasksClient client = CloudTasksClient.create()) {
      QueueName queueName =
          QueueName.newBuilder()
              .setLocation(ProjectInfo.CLOUD_REGION)
              .setProject(ProjectInfo.PROJECT_ID)
              .setQueue(ProjectInfo.QUEUE_ID)
              .build();

      CloudTasksClient.ListTasksPagedResponse listTasksPagedResponse = client.listTasks(queueName);
      listTasksPagedResponse
          .getPage()
          .getResponse()
          .getTasksList()
          .forEach(
              task -> {
                client.deleteTask(task.getName());
              });
    }
  }
}
