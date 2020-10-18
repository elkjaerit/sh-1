package dk.elkjaerit.smartheating.powerunit;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import dk.elkjaerit.smartheating.BuildingRepository;
import dk.elkjaerit.smartheating.CloudTask;
import dk.elkjaerit.smartheating.common.model.DigitalOutput;
import dk.elkjaerit.smartheating.common.model.Room;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PowerUnitUpdater {

  private static final Logger LOG = Logger.getLogger(PowerUnitUpdater.class.getName());

  public static void main(String[] args) {
    update("bbd508c9-42aa-4bc5-9981-a5b523d65aca");
  }

  public static void update() {
    BuildingRepository.getBuildings()
        .forEach(
            queryDocumentSnapshot -> {
              DocumentReference building = queryDocumentSnapshot.getReference();
              updateBuilding(building);
            });
  }

  public static void update(String buildingId) {
    DocumentReference building = BuildingRepository.getBuilding(buildingId);
    updateBuilding(building);
  }

  private static void updateBuilding(DocumentReference building) {
    try {
      updateRooms(building);

      List<Room> rooms =
              building.collection("rooms").get().get().getDocuments().stream()
                      .map(queryDocumentSnapshot -> queryDocumentSnapshot.toObject(Room.class))
                      .collect(Collectors.toList());

      sendMessageToPowerUnit(rooms);
      createTask(building, rooms);

    } catch (InterruptedException | ExecutionException | IOException e) {
      LOG.log(Level.SEVERE, "Error updating power unit", e);
    }
  }

  private static void createTask(DocumentReference building, List<Room> rooms) throws IOException {
    Optional<Room> nextRoomToBeUpdated =
        rooms.stream()
            .filter(room -> room.getDigitalOutput().getNextToggleTime() != null)
            .min(Comparator.comparing(o -> o.getDigitalOutput().getNextToggleTime()));

    if (nextRoomToBeUpdated.isPresent()) {
      com.google.protobuf.Timestamp nexUpdate =
          com.google.protobuf.Timestamp.newBuilder()
              .setSeconds(
                  nextRoomToBeUpdated.get().getDigitalOutput().getNextToggleTime().getSeconds())
              .build();

      CloudTask.scheduleTask(building.getId(), nexUpdate);
      LOG.info(
          "Scheduled next run: "
              + nextRoomToBeUpdated.get().getDigitalOutput().getNextToggleTime()
              + " - room: "
              + nextRoomToBeUpdated.get().getName());
    }
  }

  private static void updateRooms(DocumentReference building) throws InterruptedException, ExecutionException {
    List<QueryDocumentSnapshot> roomSnapshotList =
        building.collection("rooms").get().get().getDocuments();
    roomSnapshotList.forEach(PowerUnitUpdater::updateRoom);
  }

  private static void sendMessageToPowerUnit(List<Room> rooms) {
    try {
      int state = getStateAsBinary(rooms);
      PowerUnitClient.sendConfiguration("power-unit-device", String.valueOf(state));
      LOG.info(
          "Updating power unit State: "
              + StringUtils.leftPad(Integer.toBinaryString(state), 10, "0")
              + " ("
              + state
              + ")");
    } catch (GeneralSecurityException | IOException e) {
      LOG.severe("Could not send message");
    }
  }

  @SneakyThrows
  private static void updateRoom(QueryDocumentSnapshot roomSnapshot) {
    Room room = roomSnapshot.toObject(Room.class);
    room.getDigitalOutput().update(3600);
    roomSnapshot.getReference().update(Map.of("digitalOutput", room.getDigitalOutput())).get();
    LOG.info("Room updated: " + room.getName() + ", digitalOutput: " + room.getDigitalOutput());
  }

  private static int getStateAsBinary(List<Room> rooms) {
    return rooms.stream()
        .map(Room::getDigitalOutput)
        .filter(DigitalOutput::isTurnedOn)
        .map(DigitalOutput::getPinId)
        .reduce(0, BitUtils::setBit);
  }
}
