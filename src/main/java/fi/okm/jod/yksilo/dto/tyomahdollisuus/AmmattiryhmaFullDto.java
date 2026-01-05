/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.tyomahdollisuus;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.net.URI;

/** Data of the ammattiryhma. */
public record AmmattiryhmaFullDto(
    @Schema(requiredMode = RequiredMode.REQUIRED) URI uri,
    Integer mediaaniPalkka,
    Integer ylinDesiiliPalkka,
    Integer alinDesiiliPalkka,
    Integer tyollisetKokoMaa,
    String kohtaanto) {}
