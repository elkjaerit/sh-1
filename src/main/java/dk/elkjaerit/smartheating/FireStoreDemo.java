package dk.elkjaerit.smartheating;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import dk.elkjaerit.smartheating.common.model.Building;
import dk.elkjaerit.smartheating.common.model.DigitalOutput;
import dk.elkjaerit.smartheating.common.model.Room;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class FireStoreDemo {

  public static void main(String[] args)
      throws IOException, ExecutionException, InterruptedException {
    FirestoreOptions firestoreOptions =
        FirestoreOptions.getDefaultInstance()
            .toBuilder()
            .setProjectId(ProjectInfo.PROJECT_ID)
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .build();
    Firestore db = firestoreOptions.getService();

    createSomeData(db);

    //    QueryDocumentSnapshot buildingReference =
    // BuildingRepository.getBuildingByGatewayId("B93A7D80");
    //    Building building = buildingReference.toObject(Building.class);
    //    building.getRooms().get(0).setSensor(Sensor.builder().temperature(21d).build());
    //    buildingReference.getReference().set(building);
    //
    //    System.out.println(buildingReference.getId());

  }

  private static void createSomeData(Firestore db) throws InterruptedException, ExecutionException {
    CollectionReference buildings = db.collection("buildings");
    //    ApiFuture<WriteResult> set =
    //        buildings
    //            .document(UUID.randomUUID().toString())
    //            .collection("rooms")
    //            .document()
    //            .set(new Room(UUID.randomUUID().toString(), "Ida er ikke med ?",
    // "58:2D:34:35:DB:12",null, null));
    //
    //    set.get();

    Building building =
        Building.builder()
            .location(new GeoPoint(56.315498, 10.32041))
            .gatewayId("B93A7D80")
            .cityId("2619787")
            .room(
                Room.builder()
                    .id(UUID.randomUUID().toString())
                    .name("Bryggers/Entre")
                    .digitalOutput(DigitalOutput.builder().pinId(1).build())
                    .sensorId(null)
                    .build())
            .room(
                Room.builder()
                    .id(UUID.randomUUID().toString())
                    .name("Bad")
                    .digitalOutput(DigitalOutput.builder().pinId(2).build())
                    .sensorId("58:2D:34:35:AE:31")
                    .build())
            .room(
                Room.builder()
                    .id(UUID.randomUUID().toString())
                    .name("Kontor")
                    .digitalOutput(DigitalOutput.builder().pinId(3).build())
                    .sensorId("58:2D:34:35:CB:89")
                    .build())
            .room(
                Room.builder()
                    .id(UUID.randomUUID().toString())
                    .name("Stue")
                    .digitalOutput(DigitalOutput.builder().pinId(4).build())
                    .sensorId("58:2D:34:35:D5:A0")
                    .build())
            .room(
                Room.builder()
                    .id(UUID.randomUUID().toString())
                    .name("Alrum")
                    .digitalOutput(DigitalOutput.builder().pinId(5).build())
                    .sensorId("58:2D:34:35:D8:73")
                    .build())
            .room(
                Room.builder()
                    .id(UUID.randomUUID().toString())
                    .name("Lille bad")
                    .digitalOutput(DigitalOutput.builder().pinId(6).build())
                    .sensorId("58:2D:34:35:CC:C4")
                    .build())
            .room(
                Room.builder()
                    .id(UUID.randomUUID().toString())
                    .name("Mathilde")
                    .digitalOutput(DigitalOutput.builder().pinId(7).build())
                    .sensorId("58:2D:34:35:DB:07")
                    .build())
            .room(
                Room.builder()
                    .id(UUID.randomUUID().toString())
                    .name("Ida")
                    .digitalOutput(DigitalOutput.builder().pinId(8).build())
                    .sensorId("58:2D:34:35:DB:12")
                    .build())
            .build();

    ApiFuture<WriteResult> future =
        db.collection("buildings").document(UUID.randomUUID().toString()).set(building);
    //     block on response if required
    System.out.println("Update time : " + future.get().getUpdateTime());
  }
}
