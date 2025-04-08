/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili.export;

import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.PaamaaraTyyppi;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaamaaraExportDto(
    UUID id,
    Instant luotu,
    PaamaaraTyyppi tyyppi,
    UUID tyomahdollisuus,
    UUID koulutusmahdollisuus,
    List<PolunSuunnitelmaExportDto> suunnitelmat,
    LocalizedString tavoite) {}
