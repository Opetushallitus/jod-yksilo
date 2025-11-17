/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.externalapi.v1.dto;

import fi.okm.jod.yksilo.domain.TavoiteTyyppi;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record ExtTavoiteDto(
    @Schema(
            description = "This field is deprecated. Tavoite type is not differentiated anymore",
            deprecated = true)
        TavoiteTyyppi tyyppi,
    UUID tyomahdollisuusId,
    @Schema(
            description = "This field is deprecated. Tavoite can only be tyomahdollisuus",
            deprecated = true)
        UUID koulutusmahdollisuusId) {}
