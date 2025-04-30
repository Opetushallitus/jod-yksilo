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
import fi.okm.jod.yksilo.domain.PolunVaiheLahde;
import fi.okm.jod.yksilo.domain.PolunVaiheTyyppi;
import java.net.URI;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record PolunVaiheExportDto(
    UUID id,
    PolunVaiheLahde lahde,
    PolunVaiheTyyppi tyyppi,
    LocalizedString nimi,
    LocalizedString kuvaus,
    Set<String> linkit,
    LocalDate alkuPvm,
    LocalDate loppuPvm,
    Set<URI> osaamiset,
    boolean valmis) {}
