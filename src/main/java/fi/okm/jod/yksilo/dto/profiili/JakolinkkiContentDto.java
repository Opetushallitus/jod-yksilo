/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili;

import java.time.Instant;
import java.util.Set;

public record JakolinkkiContentDto(
    Instant voimassaAsti,
    String etunimi,
    String sukunimi,
    String email,
    String kotikunta,
    Integer syntymavuosi,
    Set<TyopaikkaDto> tyopaikat,
    Set<KoulutusKokonaisuusDto> koulutusKokonaisuudet,
    Set<ToimintoDto> toiminnot,
    MuuOsaaminenDto muuOsaaminen,
    Set<SuosikkiDto> suosikit,
    KiinnostuksetDto kiinnostukset) {}
