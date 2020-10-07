package dk.elkjaerit.smartheating;

import dk.elkjaerit.smartheating.common.model.Building;

public class Status {

  public static void main(String[] args) {
    BuildingRepository.getBuildingByGatewayId("B93A7D80")
        .map(queryDocumentSnapshot -> queryDocumentSnapshot.toObject(Building.class))
        .get()
        .getRooms()
        .forEach(
            room -> {
              System.out.println(
                  room.getName()
                      + "\t "
                      + room.getSensor().getTemperature()
                      + ", "
                      + room.getSensor().getLastUpdated());
            });
  }
}
