/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili;

import static java.util.Objects.requireNonNull;

import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OsaamisenLahdeDto(@NotNull OsaamisenLahdeTyyppi tyyppi, UUID id) {

  public OsaamisenLahdeDto {
    requireNonNull(tyyppi);
    requireNonNull(id);
  }
}
