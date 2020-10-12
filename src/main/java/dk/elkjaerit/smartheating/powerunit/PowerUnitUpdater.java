package dk.elkjaerit.smartheating.powerunit;

import com.google.cloud.firestore.DocumentReference;
import dk.elkjaerit.smartheating.BuildingRepository;
import dk.elkjaerit.smartheating.common.model.DigitalOutput;
import dk.elkjaerit.smartheating.common.model.Room;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
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
      long locationUpdateCount =
          building.collection("rooms").get().get().getDocuments().stream()
              .map(
                  queryDocumentSnapshot -> {
                    Room room = queryDocumentSnapshot.toObject(Room.class);
                    boolean updated = room.getDigitalOutput().update(3600);
                    if (updated) {
                      queryDocumentSnapshot.getReference().set(room);
                      return true;
                    } else {
                      return false;
                    }
                  })
              .filter(Boolean::booleanValue)
              .count();

      if (locationUpdateCount > 0) {
        List<Room> rooms =
            building.collection("rooms").get().get().getDocuments().stream()
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
    } catch (InterruptedException | ExecutionException e) {
      LOG.log(Level.SEVERE, "Error updating power unit", e);
    }
  }

  private static int getStateAsBinary(List<Room> rooms) {
    return rooms.stream()
        .map(Room::getDigitalOutput)
        .filter(DigitalOutput::isTurnedOn)
        .map(DigitalOutput::getPinId)
        .reduce(0, BitUtils::setBit);
  }
}
