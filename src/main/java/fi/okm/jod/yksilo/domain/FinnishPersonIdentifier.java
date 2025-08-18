/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.domain;

import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * Represents a Finnish person identifier (henkilötunnus).
 *
 * <p>See <a href="https://finlex.fi/fi/lainsaadanto/2010/128">Finnish legislation 128/2010</a>
 */
public final class FinnishPersonIdentifier {
  private static final String CHECKSUM_CHARS = "0123456789ABCDEFHJKLMNPRSTUVWXY";

  private final LocalDate birthDate;
  private final int personNumber;
  private final char separator;

  private FinnishPersonIdentifier(LocalDate birthDate, char separator, int personNumber) {
    this.birthDate = birthDate;
    this.personNumber = personNumber;
    this.separator = separator;
  }

  public int getBirthYear() {
    return birthDate.getYear();
  }

  /**
   * Returns the National Identification code as a string.
   *
   * <p>Deliberately not using toString() to avoid accidentally exposing the identifier in logs or *
   * other outputs.
   */
  public String asString() {
    var dateNumber =
        birthDate.getDayOfMonth() * 10000
            + birthDate.getMonthValue() * 100
            + (birthDate.getYear() % 100);
    return String.format(
        "%06d%c%03d%c",
        dateNumber, separator, personNumber, checksum(dateNumber * 1000 + personNumber));
  }

  public Sukupuoli getGender() {
    return personNumber % 2 == 0 ? Sukupuoli.NAINEN : Sukupuoli.MIES;
  }

  public static FinnishPersonIdentifier of(String identifier) {
    if (identifier == null || identifier.length() != 11) {
      throw new IllegalArgumentException("Invalid person identifier");
    }

    var dateNumber = Integer.parseInt(identifier, 0, 6, 10);
    var personNumber = Integer.parseInt(identifier, 7, 10, 10);

    var day = dateNumber / 10000;
    var month = (dateNumber / 100) % 100;
    var separator = identifier.charAt(6);
    var year = century(separator) + (dateNumber % 100);

    if (identifier.charAt(10) != checksum(dateNumber * 1000 + personNumber)) {
      throw new IllegalArgumentException("Invalid checksum");
    }

    try {
      return new FinnishPersonIdentifier(LocalDate.of(year, month, day), separator, personNumber);
    } catch (DateTimeException e) {
      throw new IllegalArgumentException("Invalid date in person identifier");
    }
  }

  private static char checksum(int value) {
    return CHECKSUM_CHARS.charAt(value % 31);
  }

  private static int century(char separator) {
    return switch (separator) {
      case 'A', 'B', 'C', 'D', 'E', 'F' -> 2000;
      case 'Y', 'X', 'W', 'V', 'U', '-' -> 1900;
      case '+' -> 1800;
      default -> throw new IllegalArgumentException("Invalid separator: " + separator);
    };
  }
}
