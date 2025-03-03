/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import fi.okm.jod.yksilo.validation.PrintableString;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PolunSuunnitelmaYhteenvetoDto(
    @Null(groups = Add.class) @Schema(accessMode = READ_ONLY) UUID id,
    @NotEmpty @PrintableString @Size(max = 200) LocalizedString nimi) {}
