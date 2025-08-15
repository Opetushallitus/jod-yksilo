/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.admin.dto;

import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.LocalizedString;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Koulutusmahdollisuus that is returned to frontend */
public record KoulutusMahdollisuusResponseDto(
    UUID id,
    Instant luomisaika,
    LocalizedString otsikko,
    LocalizedString tiivistelma,
    LocalizedString kuvaus,
    KoulutusmahdollisuusTyyppi tyyppi,
    List<KoulutusResponseDto> koulutukset) {}
