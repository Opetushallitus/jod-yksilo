/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.externalapi.v1.dto;

import java.util.Set;
import java.util.UUID;

public record ExtProfiiliDto(
    UUID id,
    Set<ExtYksilonOsaaminenDto> yksilonOsaamiset,
    Set<ExtOsaamisKiinnostusDto> osaamisKiinnostukset,
    Set<ExtAmmattiKiinnostusDto> ammattiKiinnostukset,
    Set<ExtSuosikkiDto> suosikit,
    Set<ExtPaamaaraDto> paamaarat) {}
