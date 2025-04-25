/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import fi.okm.jod.yksilo.domain.SuosikkiTyyppi;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.time.Instant;
import java.util.UUID;

public record SuosikkiDto(
    @Null(groups = Add.class) @Schema(accessMode = READ_ONLY) UUID id,
    @NotNull UUID kohdeId,
    @NotNull SuosikkiTyyppi tyyppi,
    @Null(groups = Add.class) @Schema(accessMode = READ_ONLY) Instant luotu) {}
