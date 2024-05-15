/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

interface ValidInterval {
  LocalDate alkuPvm();

  LocalDate loppuPvm();

  @JsonIgnore
  @AssertTrue(message = "Invalid interval")
  default boolean isValidInterval() {
    return loppuPvm() == null || (alkuPvm() != null && !loppuPvm().isBefore(alkuPvm()));
  }
}
