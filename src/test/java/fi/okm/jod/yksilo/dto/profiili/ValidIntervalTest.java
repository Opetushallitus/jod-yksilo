/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ValidIntervalTest {
  @Test
  void test() {
    record Test(LocalDate alkuPvm, LocalDate loppuPvm) implements ValidInterval {}

    assertFalse(new Test(LocalDate.of(2021, 1, 1), LocalDate.of(2020, 1, 1)).isValidInterval());
    assertFalse(new Test(null, LocalDate.of(2020, 1, 1)).isValidInterval());

    assertTrue(new Test(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1)).isValidInterval());
    assertTrue(new Test(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 1)).isValidInterval());
    assertTrue(new Test(LocalDate.of(2021, 1, 1), null).isValidInterval());
    assertTrue(new Test(null, null).isValidInterval());
  }
}
