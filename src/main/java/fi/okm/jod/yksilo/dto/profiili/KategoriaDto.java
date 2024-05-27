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
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.validation.PrintableString;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record KategoriaDto(
    UUID id,
    @Size(max = 200) @PrintableString LocalizedString nimi,
    @Size(max = 10000) @PrintableString LocalizedString kuvaus) {

  @JsonIgnore
  @AssertTrue(message = "nimi is required")
  public boolean isValid() {
    return id != null || nimi != null && !nimi.isEmpty();
  }
}
