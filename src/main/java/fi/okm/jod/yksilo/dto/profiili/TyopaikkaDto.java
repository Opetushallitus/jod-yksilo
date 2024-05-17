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
import fi.okm.jod.yksilo.dto.validationgroup.Update;
import fi.okm.jod.yksilo.validation.Limits;
import fi.okm.jod.yksilo.validation.PrintableString;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record TyopaikkaDto(
    @Null(groups = Add.class) @NotNull(groups = Update.class) UUID id,
    @NotEmpty @Size(max = 200) @PrintableString LocalizedString nimi,
    @Size(max = Limits.TOIMENKUVA_PER_TYOPAIKKA) Set<@Valid ToimenkuvaDto> toimenkuvat) {}
