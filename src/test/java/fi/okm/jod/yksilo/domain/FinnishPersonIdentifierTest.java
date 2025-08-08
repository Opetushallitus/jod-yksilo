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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FinnishPersonIdentifierTest {

  @ParameterizedTest
  @DisplayName("Should parse valid 21st century identifier")
  @ValueSource(chars = {'A', 'B', 'C', 'D', 'E', 'F'})
  void shouldParseValid21stCenturyIdentifier(char separator) {
    var identifier = FinnishPersonIdentifier.of("010199" + separator + "9986");
    assertEquals(2099, identifier.getBirthYear());
    assertEquals(Sukupuoli.NAINEN, identifier.getGender());
  }

  @ParameterizedTest
  @DisplayName("Should parse valid 20th century identifier")
  @ValueSource(chars = {'Y', 'X', 'W', 'V', 'U', '-'})
  void shouldParseValid20thCenturyIdentifier(char separator) {
    var identifier = FinnishPersonIdentifier.of("010199" + separator + "9997");
    assertEquals(1999, identifier.getBirthYear());
    assertEquals(Sukupuoli.MIES, identifier.getGender());
  }

  @Test
  @DisplayName("Should parse valid 19th century identifier")
  void shouldParseValid19thCenturyIdentifier() {
    var identifier = FinnishPersonIdentifier.of("010199+9986");
    assertEquals(1899, identifier.getBirthYear());
    assertEquals(Sukupuoli.NAINEN, identifier.getGender());
  }

  @Test
  @DisplayName("Should throw exception for null identifier")
  void shouldThrowExceptionForNullIdentifier() {
    assertThrows(IllegalArgumentException.class, () -> FinnishPersonIdentifier.of(null));
  }

  @ParameterizedTest
  @DisplayName("Should throw exception for malformed identifiers")
  @ValueSource(
      strings = {
        "", // Empty string
        "12345", // Too short
        "010199A9997X", // Too long
        "AB0199A999H", // Invalid date part
        "010199!9997", // Invalid separator
        "010199A99H", // Missing digit in person number
        "010199AABCH", // Invalid person number
        "320199-9997", // Invalid day
        "011399+999W", // Invalid month
        "010199A999G" // Invalid checksum character
      })
  void shouldThrowExceptionForMalformedIdentifiers(String identifier) {
    assertThrows(IllegalArgumentException.class, () -> FinnishPersonIdentifier.of(identifier));
  }

  @ParameterizedTest
  @DisplayName("Should throw exception for invalid checksum")
  @ValueSource(
      chars = {
        '0', '1', '2', '3', '4', '5', '6', /*'7',*/ '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'H',
        'J', 'K', 'L', 'M', 'N', 'P', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y'
      })
  void shouldThrowExceptionForInvalidChecksum(char checksum) {
    // Valid format but wrong checksum (should be 7)
    assertThrows(
        IllegalArgumentException.class, () -> FinnishPersonIdentifier.of("010199-999" + checksum));
  }

  @Test
  @DisplayName("Should render identifier correctly")
  void shouldRenderIdentifierCorrectly() {
    var identifier = FinnishPersonIdentifier.of("010199A9986");
    assertEquals("010199A9986", identifier.asString());
  }
}
