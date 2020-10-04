package dk.elkjaerit.smartheating.powerunit;

public class BitUtils {

  /**
   * Verifies if bit value is set
   *
   * @param value
   * @param pos one based position
   * @return
   */
  public static boolean checkBit(int value, int pos) {
    return (value & 1 << (pos - 1)) != 0;
  }

  /**
   * Sets bit value in integer
   *
   * @param value
   * @param pos one base position
   * @return
   */
  public static int setBit(int value, int pos) {
    value = value | (1 << (pos - 1));
    return value;
  }
}
