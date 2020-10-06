package dk.elkjaerit.smartheating;

public class Status {

  public static void main(String[] args) {
    BuildingRepository.findBuildingByGateway("B93A7D80")
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
