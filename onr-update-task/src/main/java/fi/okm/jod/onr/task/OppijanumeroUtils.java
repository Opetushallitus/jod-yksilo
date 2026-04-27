/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.onr.task;

/** Utilities for validating and qualifying oppijanumero OIDs. */
final class OppijanumeroUtils {

  static final String PREFIX = "ONR:";
  static final String HENKILO_OID_NODE = ".24.";

  private OppijanumeroUtils() {}

  /** Validates the raw oppijanumero and returns the ONR:-prefixed value. */
  static String qualify(String rawOppijanumero, String oidPrefix) {
    if (!isValid(oidPrefix, rawOppijanumero)) {
      throw new IllegalArgumentException(
          "Invalid oppijanumero format: expected OID starting with " + oidPrefix);
    }
    return PREFIX + rawOppijanumero;
  }

  static boolean isValid(String oidPrefix, String rawOppijanumero) {
    if (rawOppijanumero == null || !rawOppijanumero.startsWith(oidPrefix)) {
      return false;
    }
    long value;
    try {
      value = Long.parseLong(rawOppijanumero, oidPrefix.length(), rawOppijanumero.length(), 10);
    } catch (NumberFormatException _) {
      return false;
    }
    if (value < 10_000_000_000L || value > 99_999_999_999L) {
      return false;
    }
    int checkDigit = (int) (value % 10);
    long payload = value / 10;
    boolean useIbm = oidPrefix.endsWith(HENKILO_OID_NODE);
    int expected = useIbm ? ibmChecksum(payload) : luhnChecksum(payload);
    return checkDigit == expected;
  }

  static int ibmChecksum(long payload) {
    final int[] weights = {7, 3, 1};
    int sum = 0;
    for (int j = 0; payload > 0; payload /= 10, j++) {
      sum += (int) (payload % 10) * weights[j % 3];
    }
    return (10 - sum % 10) % 10;
  }

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
