/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.domain;

public final class OppijanumeroUtils {

  public static final String PREFIX = "ONR:";
  public static final String HENKILO_OID_NODE = ".24.";

  private OppijanumeroUtils() {}

  /**
   * Validates the raw oppijanumero against the given OID prefix and returns the ONR:-prefixed
   * value.
   */
  public static String qualify(String rawOppijanumero, String oidPrefix) {
    if (!isValid(oidPrefix, rawOppijanumero)) {
      throw new IllegalArgumentException(
          "Invalid oppijanumero format: expected OID starting with " + oidPrefix);
    }
    return PREFIX + rawOppijanumero;
  }

  /**
   * Returns true if the raw ONR starts with the given OID prefix, followed by an 11-digit numeric
   * suffix whose last digit is a valid checksum.
   *
   * <p>The checksum algorithm is selected based on the last OID node in the prefix: node "24" uses
   * the "Finnish reference number" (aka "IBM") checksum, all other nodes use the Luhn checksum.
   *
   * @see <a
   *     href="https://wiki.eduuni.fi/spaces/ophPPK/pages/338171491/Oppijanumeron+solmuluokka">OPH
   *     documentation</a>
   * @see <a href="https://en.wikipedia.org/wiki/Luhn_algorithm">Luhn algorithm</a>
   * @see <a
   *     href="https://www.finanssiala.fi/wp-content/uploads/2021/03/kotimaisen_viitteen_rakenneohje.pdf">Finnish
   *     reference number checksum</a>
   */
  public static boolean isValid(String oidPrefix, String rawOppijanumero) {
    if (rawOppijanumero == null || !rawOppijanumero.startsWith(oidPrefix)) {
      return false;
    }
    long value;
    try {
      value = Long.parseLong(rawOppijanumero, oidPrefix.length(), rawOppijanumero.length(), 10);
    } catch (NumberFormatException _) {
      return false;
    }

    // Valid range: 10000000000–99999999999 (11 digits, no leading zeros)
    if (value < 10_000_000_000L || value > 99_999_999_999L) {
      return false;
    }

    int checkDigit = (int) (value % 10);
    long payload = value / 10;

    boolean useIbm = oidPrefix.endsWith(HENKILO_OID_NODE);
    int expected = useIbm ? ibmChecksum(payload) : luhnChecksum(payload);
    return checkDigit == expected;
  }

  /** Computes the IBM (Finnish reference number) checksum for the given payload. */
  static int ibmChecksum(long payload) {
    final int[] weights = {7, 3, 1};
    int sum = 0;
    for (int j = 0; payload > 0; payload /= 10, j++) {
      sum += (int) (payload % 10) * weights[j % 3];
    }
    return (10 - sum % 10) % 10;
  }

  /** Computes the Luhn checksum for the given payload. */
  static int luhnChecksum(long payload) {
    final int[] weights = {2, 1};
    int sum = 0;
    for (int j = 0; payload > 0; payload /= 10, j++) {
      int n = (int) (payload % 10) * weights[j % 2];
      if (n > 9) {
        n -= 9;
      }
      sum += n;
    }
    return (10 - sum % 10) % 10;
  }
}
