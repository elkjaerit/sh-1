package dk.elkjaerit.smartheating.powerunit;

import com.google.cloud.Timestamp;
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

import static java.util.stream.Collectors.toList;

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
      List<Room> roomsAfterUpdate = updateRooms(building);
      sendMessageToPowerUnit(building.get().get().getString("powerUnitId"), roomsAfterUpdate);
      createTask(building, roomsAfterUpdate);

    } catch (InterruptedException | ExecutionException | IOException e) {
      LOG.log(Level.SEVERE, "Error updating power unit", e);
    }
  }

  private static void createTask(DocumentReference building, List<Room> nextUpdate)
      throws IOException {
    Optional<Room> nextRoomToBeUpdated =
            nextUpdate.stream()
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

  private static List<Room> updateRooms(DocumentReference building)
      throws InterruptedException, ExecutionException {
    List<QueryDocumentSnapshot> roomSnapshotList =
        building.collection("rooms").get().get().getDocuments();
    return roomSnapshotList.stream().map(PowerUnitUpdater::updateRoom).collect(toList());
  }

  private static void sendMessageToPowerUnit(String powerUnitId, List<Room> rooms) {
    try {
      int state = getStateAsBinary(rooms);
      PowerUnitClient.sendConfiguration(powerUnitId, String.valueOf(state));
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
  private static Room updateRoom(QueryDocumentSnapshot roomSnapshot) {
    Room room = roomSnapshot.toObject(Room.class);
    room.getDigitalOutput().update(3600);
    roomSnapshot.getReference().update(Map.of("digitalOutput", room.getDigitalOutput()));
    LOG.info("Room updated: " + room.getName() + ", digitalOutput: " + room.getDigitalOutput());
    return room;
  }

  private static int getStateAsBinary(List<Room> rooms) {
    return rooms.stream()
        .map(Room::getDigitalOutput)
        .filter(DigitalOutput::isTurnedOn)
        .map(DigitalOutput::getPinId)
        .reduce(0, BitUtils::setBit);
  }
}
