/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class OppijanumeroUtilsTest {

  private static final String OID_PREFIX = "1.2.246.562.";
  private static final String TEST_OID_PREFIX = OID_PREFIX + "98.";
  private static final String HENKILO_OID_PREFIX = OID_PREFIX + "24.";

  @Test
  void ibmChecksumShouldMatchTestVectors() {
    assertEquals(4, OppijanumeroUtils.ibmChecksum(617435L));
    assertEquals(0, OppijanumeroUtils.ibmChecksum(1111111111L));
  }

  @Test
  void isValidShouldRejectMismatchedPrefix() {
    assertFalse(OppijanumeroUtils.isValid(TEST_OID_PREFIX, "1.2.246.562.24.10000000003"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1.2.246.562.98.98765432103", // Luhn checksum (also valid for IBM)
        "1.2.246.562.98.10000000009", // Luhn checksum
        "1.2.246.562.98.12345678903", // Luhn checksum
        "1.2.246.562.98.11111111115", // Luhn checksum
        "1.2.246.562.24.10000000003", // IBM checksum
        "1.2.246.562.24.12345678907", // IBM checksum
        "1.2.246.562.24.98765432103", // IBM checksum (also valid for Luhn)
      })
  void isValidShouldAcceptValidOppijanumero(String input) {
    assertTrue(OppijanumeroUtils.isValid(OID_PREFIX, input));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1.2.246.562.24.10000000003", // IBM checksum
        "1.2.246.562.24.12345678907", // IBM checksum
        "1.2.246.562.24.98765432103", // IBM checksum (also valid for Luhn)
      })
  void isValidShouldAcceptValidIbmOppijanumero(String input) {
    assertTrue(OppijanumeroUtils.isValid(HENKILO_OID_PREFIX, input));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1.2.246.562.98.10000000003", // IBM checksum, not Luhn (Luhn would be ...9)
        "1.2.246.562.98.12345678907", // IBM checksum, not Luhn (Luhn would be ...3)
      })
  void isValidShouldRejectIbmChecksumForNon24Prefix(String input) {
    assertFalse(OppijanumeroUtils.isValid(TEST_OID_PREFIX, input));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1.2.246.562.24.10000000009", // Luhn checksum, not IBM (IBM would be ...3)
        "1.2.246.562.24.11111111115", // Luhn checksum, not IBM (IBM would be ...0)
      })
  void isValidShouldRejectLuhnChecksumFor24Prefix(String input) {
    assertFalse(OppijanumeroUtils.isValid(HENKILO_OID_PREFIX, input));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "1.2.246.562.25.12345678901", // wrong node class
        "1.2.246.562.98.01000000009", // leading zero
        "1.2.246.562.98.", // missing suffix
        "1.2.246.562.98.1000000009", // too short
        "1.2.246.562.98.aaaaaaaaaaa", // non-numeric suffix
        "1.2.246.562.98.10000ab0007", // partially numeric suffix
        "FIN:010199-9986", // not an OID at all
      })
  void isValidShouldRejectInvalidFormat(String input) {
    assertFalse(OppijanumeroUtils.isValid(TEST_OID_PREFIX, input));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1.2.246.562.98.12345678908", // correct Luhn would be ...3
        "1.2.246.562.98.10000000004", // correct Luhn would be ...9
        "1.2.246.562.98.98765432104", // correct Luhn would be ...3
        "1.2.246.562.98.11111111112", // correct Luhn would be ...5
      })
  void isValidShouldRejectWrongLuhnChecksum(String input) {
    assertFalse(OppijanumeroUtils.isValid(TEST_OID_PREFIX, input));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1.2.246.562.24.12345678908", // correct IBM would be ...7
        "1.2.246.562.24.10000000004", // correct IBM would be ...3
        "1.2.246.562.24.98765432104", // correct IBM would be ...3
      })
  void isValidShouldRejectWrongIbmChecksum(String input) {
    assertFalse(OppijanumeroUtils.isValid(HENKILO_OID_PREFIX, input));
  }
}
