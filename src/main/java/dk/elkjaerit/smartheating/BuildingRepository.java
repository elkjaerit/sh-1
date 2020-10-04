package dk.elkjaerit.smartheating;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import dk.elkjaerit.smartheating.model.Building;

import java.io.IOException;
import java.util.Comparator;
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

  public static void main(String[] args) {
    findBuildingByGateway("B93A7D80").get().getRooms().stream()
        .sorted(Comparator.comparingInt(value -> value.getDigitalOutput().getPinId()))
        .forEach(
            room ->
                System.out.println(room.getName() + ": " + room.getDigitalOutput().isTurnedOn()));
  }

  public static QuerySnapshot getBuildings() {
    try {
      return db.collection("buildings").get().get();
    } catch (InterruptedException | ExecutionException e) {
      LOG.severe("Could not get buildings");
      throw new RuntimeException("Could not get buildings", e);
    }
  }

  public static Optional<Building> findBuildingByGateway(String gatewayId) {
    try {
      ApiFuture<QuerySnapshot> querySnapshotApiFuture =
          db.collection("buildings").whereArrayContains("gatewayIds", gatewayId).get();
      QuerySnapshot queryDocumentSnapshots = querySnapshotApiFuture.get();

      List<Building> buildings = db.collection("buildings").get().get().toObjects(Building.class);
      return buildings.stream()
          .filter(building -> building.getGatewayIds().contains(gatewayId))
          .findFirst();
    } catch (Exception e) {
      LOG.severe("Could not find building for sensor: " + gatewayId);
      return Optional.empty();
    }
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
}
