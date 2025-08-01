/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.externalapi.v1.dto;

import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import java.util.UUID;

/** Tiedot mitä koulutusmahdollisuudesta palautetaan ulkoisen rajapinnan v1-versiossa. */
public record ExtKoulutusMahdollisuusDto(
    UUID id,
    LocalizedString otsikko,
    LocalizedString tiivistelma,
    LocalizedString kuvaus,
    Koulutusmahdollisuus.KestoJakauma kesto,
    boolean aktiivinen) {}
