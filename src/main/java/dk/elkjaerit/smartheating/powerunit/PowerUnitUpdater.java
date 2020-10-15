package dk.elkjaerit.smartheating.powerunit;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import dk.elkjaerit.smartheating.BuildingRepository;
import dk.elkjaerit.smartheating.common.model.DigitalOutput;
import dk.elkjaerit.smartheating.common.model.Room;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PowerUnitUpdater {

  private static final Logger LOG = Logger.getLogger(PowerUnitUpdater.class.getName());

  public static void main(String[] args) {
    update();
  }

  public static void update() {
    BuildingRepository.getBuildings()
        .forEach(
            queryDocumentSnapshot -> {
              DocumentReference building = queryDocumentSnapshot.getReference();
              updateBuilding(building);
            });
  }

  private static void updateBuilding(DocumentReference building) {
    try {
      List<QueryDocumentSnapshot> roomSnapshotList =
          building.collection("rooms").get().get().getDocuments();
      boolean isRoomUpdated = roomSnapshotList.stream().anyMatch(PowerUnitUpdater::updateRoom);

      if (isRoomUpdated) {
        sendMessageToPowerUnit(roomSnapshotList);
      }
    } catch (InterruptedException | ExecutionException e) {
      LOG.log(Level.SEVERE, "Error updating power unit", e);
    }
  }

  private static void sendMessageToPowerUnit(List<QueryDocumentSnapshot> roomSnapshotList) {
    List<Room> rooms =
        roomSnapshotList.stream()
            .map(queryDocumentSnapshot -> queryDocumentSnapshot.toObject(Room.class))
            .collect(Collectors.toList());

    int state = getStateAsBinary(rooms);

    try {
      PowerUnitClient.sendConfiguration("power-unit-device", String.valueOf(state));
    } catch (GeneralSecurityException | IOException e) {
      LOG.severe("Could not send message");
    }
    LOG.info("Updating power unit State: " + state);
  }

  private static boolean updateRoom(QueryDocumentSnapshot roomSnapshot) {
    Room room = roomSnapshot.toObject(Room.class);
    boolean updated = room.getDigitalOutput().update(3600);
    if (updated) {
      LOG.info("Room updated: " + room.getName() + ", digitalOutput: " + room.getDigitalOutput());
      roomSnapshot.getReference().update(Map.of("digitalOutput", room.getDigitalOutput()));
    }
    return updated;
  }

  private static int getStateAsBinary(List<Room> rooms) {
    return rooms.stream()
        .map(Room::getDigitalOutput)
        .filter(DigitalOutput::isTurnedOn)
        .map(DigitalOutput::getPinId)
        .reduce(0, BitUtils::setBit);
  }
}
