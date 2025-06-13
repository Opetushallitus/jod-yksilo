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
import java.net.URI;
import java.util.Set;
import java.util.UUID;

public record YksiloExportDto(
    UUID id,
    Boolean tervetuloapolku,
    Boolean lupaLuovuttaaTiedotUlkopuoliselle,
    Boolean lupaArkistoida,
    Boolean lupaKayttaaTekoalynKoulutukseen,
    Set<TyopaikkaExportDto> tyopaikat,
    Set<KoulutusKokonaisuusExportDto> koulutusKokonaisuudet,
    Set<ToimintoExportDto> toiminnot,
    Set<URI> osaamisKiinnostukset,
    Set<URI> ammattiKiinnostukset,
    LocalizedString muuOsaaminenVapaateksti,
    LocalizedString osaamisKiinnostuksetVapaateksti,
    Set<YksilonSuosikkiExportDto> suosikit,
    Set<PaamaaraExportDto> paamaarat) {}
