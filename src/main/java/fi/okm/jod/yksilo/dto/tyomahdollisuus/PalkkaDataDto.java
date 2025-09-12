/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.tyomahdollisuus;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record PalkkaDataDto(
    @NotNull Instant tiedotHaettu,
    Integer mediaaniPalkka,
    Integer ylinDesiiliPalkka,
    Integer alinDesiiliPalkka) {}
