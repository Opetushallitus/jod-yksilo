/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili;

import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import fi.okm.jod.yksilo.validation.PrintableString;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record KoulutusDto(
    @Null(groups = Add.class) UUID id,
    @NotEmpty @Size(max = 200) @PrintableString LocalizedString nimi,
    @Size(max = 10000) @PrintableString LocalizedString kuvaus,
    LocalDate alkuPvm,
    LocalDate loppuPvm)
    implements ValidInterval {

  public KoulutusDto(
      LocalizedString nimi, LocalizedString kuvaus, LocalDate alkuPvm, LocalDate loppuPvm) {
    this(null, nimi, kuvaus, alkuPvm, loppuPvm);
  }
}
