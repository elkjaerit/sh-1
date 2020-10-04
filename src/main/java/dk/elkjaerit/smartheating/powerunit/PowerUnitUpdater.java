package dk.elkjaerit.smartheating.powerunit;

import dk.elkjaerit.smartheating.BuildingRepository;
import dk.elkjaerit.smartheating.model.Building;
import dk.elkjaerit.smartheating.model.DigitalOutput;
import dk.elkjaerit.smartheating.model.Room;
import org.apache.commons.lang3.RandomUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;

public class PowerUnitUpdater {

  private static final Logger LOG = Logger.getLogger(PowerUnitUpdater.class.getName());

  public static void main(String[] args) {
    update();
  }

  public static void update() {
    BuildingRepository.getBuildings()
        .forEach(
            queryDocumentSnapshot -> {
              Building building = queryDocumentSnapshot.toObject(Building.class);
              updateBuilding(building);
              queryDocumentSnapshot.getReference().set(building);
            });
  }

  private static void updateBuilding(Building building) {
    long locationUpdateCount =
        building.getRooms().stream()
            .map(room -> room.getDigitalOutput().update(3600))
            .filter(Boolean::booleanValue)
            .count();

    if (locationUpdateCount > 0) {
      int state = getStateAsBinary(building);

      try {
        PowerUnitClient.sendConfiguration("power-unit-device",
                String.valueOf(state));

      } catch (GeneralSecurityException | IOException e) {
        LOG.severe("Could not send message");
      }

      LOG.info("Updating power unit State: " + state);
    }
  }

  private static int getStateAsBinary(Building building) {
    return building.getRooms().stream()
        .map(Room::getDigitalOutput)
        .filter(DigitalOutput::isTurnedOn)
        .map(DigitalOutput::getPinId)
        .reduce(0, BitUtils::setBit);
  }
}
