package dk.elkjaerit.smartheating;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class BuildingRepository {

  private static final Logger LOG = Logger.getLogger(BuildingRepository.class.getName());
  private static Firestore db;

  static {
    try {
      FirestoreOptions firestoreOptions =
          FirestoreOptions.getDefaultInstance()
              .toBuilder()
              .setProjectId(ProjectInfo.PROJECT_ID)
              .setCredentials(GoogleCredentials.getApplicationDefault())
              .build();

      db = firestoreOptions.getService();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static QuerySnapshot getBuildings() {
    try {
      return db.collection("buildings").get().get();
    } catch (InterruptedException | ExecutionException e) {
      LOG.severe("Could not get buildings");
      throw new RuntimeException("Could not get buildings", e);
    }
  }

  public static DocumentReference getBuilding(String buildingId) {
      return db.document("buildings/" + buildingId);
  }

  public static Optional<QueryDocumentSnapshot> getBuildingByGatewayId(String gatewayId) {
    ApiFuture<QuerySnapshot> querySnapshotApiFuture =
        db.collection("buildings").whereArrayContains("gatewayIds", gatewayId).get();
    try {
      return querySnapshotApiFuture.get().getDocuments().stream().findFirst();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Error getting building with gateWayId: " + gatewayId);
    }
  }

  public static List<QueryDocumentSnapshot> getRooms(String gatewayId)
      throws InterruptedException, ExecutionException {
    return BuildingRepository.getBuildingByGatewayId(gatewayId)
        .get()
        .getReference()
        .collection("rooms")
        .get()
        .get()
        .getDocuments();
  }

  public static Optional<QueryDocumentSnapshot> getRoomBySensorId(
      DocumentReference building, String sensorId) {

    Query query = building.collection("rooms").whereEqualTo("sensorId", sensorId);
    try {
      List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();
      if (documents.size() > 1) {
        throw new IllegalArgumentException("Found too many rooms");
      }
      return documents.stream().findFirst();

    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }
}
