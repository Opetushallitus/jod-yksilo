/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto;

import fi.okm.jod.yksilo.domain.LocalizedString;
import jakarta.validation.constraints.NotNull;
import java.net.URI;

public record AmmattiDto(
    @NotNull URI uri,
    @NotNull String koodi,
    @NotNull LocalizedString nimi,
    LocalizedString kuvaus) {}
