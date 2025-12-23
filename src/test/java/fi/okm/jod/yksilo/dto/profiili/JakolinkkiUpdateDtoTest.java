/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JakolinkkiUpdateDtoTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void shouldReturnTrueForIsExpiresAtWithinOneYearWhenVoimassaAstiIsNull() {
    JakolinkkiUpdateDto dto = createDto(null);
    assertTrue(dto.isExpiresAtWithinOneYear());
  }

  @Test
  void shouldReturnTrueForIsExpiresAtWithinOneYearWhenVoimassaAstiIsWithinOneYear() {
    Instant now = Instant.now();
    // 365 days is generally within a year (or exactly a year)
    Instant withinOneYear = now.plus(360, ChronoUnit.DAYS);
    JakolinkkiUpdateDto dto = createDto(withinOneYear);
    assertTrue(dto.isExpiresAtWithinOneYear());
  }

  @Test
  void shouldReturnTrueForIsExpiresAtWithinOneYearWhenVoimassaAstiIsOneYear() {
    Instant oneYear =
        java.time.OffsetDateTime.now(java.time.Clock.systemUTC()).plusYears(1).toInstant();
    JakolinkkiUpdateDto dto = createDto(oneYear);
    assertTrue(dto.isExpiresAtWithinOneYear());
  }

  @Test
  void shouldReturnFalseForIsExpiresAtWithinOneYearWhenVoimassaAstiIsMoreThanOneYear() {
    Instant now = Instant.now();
    // 367 days is definitely more than a year
    Instant moreThanOneYear = now.plus(367, ChronoUnit.DAYS);
    JakolinkkiUpdateDto dto = createDto(moreThanOneYear);
    assertFalse(dto.isExpiresAtWithinOneYear());
  }

  @Test
  void shouldValidateSuccessfullyWhenVoimassaAstiIsFuture() {
    Instant future = Instant.now().plus(1, ChronoUnit.DAYS);
    JakolinkkiUpdateDto dto = createDto(future);
    Set<ConstraintViolation<JakolinkkiUpdateDto>> violations = validator.validate(dto);
    assertTrue(violations.isEmpty());
  }

  @Test
  void shouldFailValidationWhenVoimassaAstiIsPast() {
    Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
    JakolinkkiUpdateDto dto = createDto(past);
    Set<ConstraintViolation<JakolinkkiUpdateDto>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("voimassaAsti")));
  }

  @Test
  void shouldFailValidationWhenVoimassaAstiIsMoreThanOneYear() {
    Instant moreThanOneYear = Instant.now().plus(367, ChronoUnit.DAYS);
    JakolinkkiUpdateDto dto = createDto(moreThanOneYear);
    Set<ConstraintViolation<JakolinkkiUpdateDto>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty());
    // The custom validation method isExpiresAtWithinOneYear is annotated with @AssertTrue
    // The method name is isExpiresAtWithinOneYear, so the property name in violation is
    // expiresAtWithinOneYear
    assertTrue(
        violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("expiresAtWithinOneYear")));
  }

  private JakolinkkiUpdateDto createDto(Instant voimassaAsti) {
    return new JakolinkkiUpdateDto(
        null,
        null,
        "test",
        voimassaAsti,
        "note",
        false,
        false,
        false,
        false,
        false,
        false,
        Collections.emptySet(),
        Collections.emptySet(),
        Collections.emptySet(),
        Collections.emptySet(),
        Collections.emptySet());
  }
}
