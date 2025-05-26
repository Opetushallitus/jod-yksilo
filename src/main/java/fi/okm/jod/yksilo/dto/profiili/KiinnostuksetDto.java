/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili;

import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.validation.PrintableString;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Set;
import lombok.Builder;

@Builder
public record KiinnostuksetDto(
    Set<@Valid URI> kiinnostukset,
    @Size(max = 10000) @PrintableString LocalizedString vapaateksti) {}
