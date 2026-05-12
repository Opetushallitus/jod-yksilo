/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.mpassid.mock;

final class OnrUtils {

  /** OID prefix for test oppijanumeros (node 98 → Luhn checksum). */
  static final String OID_PREFIX = "1.2.246.562.98.";

  /** Builds a full oppijanumero OID from a 10-digit payload by appending a Luhn checksum digit. */
  static String onr(long payload) {
    return OID_PREFIX + payload + luhnChecksum(payload);
  }

  /** Generates a deterministic OID from hetu using hash + Luhn checksum. */
  static String generateOid(String hetu) {
    long hash = hetu.hashCode();
    long number = 1_000_000_000L + Math.abs(hash) % 9_000_000_000L;
    return OID_PREFIX + number + luhnChecksum(number);
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
