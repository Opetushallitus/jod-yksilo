/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.externalapi.v1.dto;

import fi.okm.jod.yksilo.entity.*;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import java.util.Set;
import java.util.stream.Collectors;

/** */
public class ExtAPIV1Mapper {
  private ExtAPIV1Mapper() {}

  public static ExtTyoMahdollisuusDto toTyoMahdollisuusDto(final Tyomahdollisuus tyomahdollisuus) {
    return new ExtTyoMahdollisuusDto(
        tyomahdollisuus.getId(),
        tyomahdollisuus.getOtsikko(),
        tyomahdollisuus.getKuvaus(),
        tyomahdollisuus.getTiivistelma(),
        tyomahdollisuus.getAmmattiryhma(),
        tyomahdollisuus.isAktiivinen());
  }

  public static ExtKoulutusMahdollisuusDto toKoulutusMahdollisuusDto(
      Koulutusmahdollisuus koulutusmahdollisuus) {
    return new ExtKoulutusMahdollisuusDto(
        koulutusmahdollisuus.getId(),
        koulutusmahdollisuus.getOtsikko(),
        koulutusmahdollisuus.getTiivistelma(),
        koulutusmahdollisuus.getKuvaus(),
        koulutusmahdollisuus.getKesto(),
        koulutusmahdollisuus.isAktiivinen());
  }

  public static ExtProfiiliDto toProfiiliDto(Yksilo yksilo) {
    Set<ExtYksilonOsaaminenDto> yksilonOsaamiset =
        yksilo.getOsaamiset().stream()
            .map(ExtAPIV1Mapper::toYksilonOsaaminen)
            .collect(Collectors.toSet());
    Set<ExtOsaamisKiinnostusDto> osaamisKiinnostukset =
        yksilo.getOsaamisKiinnostukset().stream()
            .map(ExtAPIV1Mapper::toOsaamisKiinnostus)
            .collect(Collectors.toSet());
    Set<ExtAmmattiKiinnostusDto> ammattiKiinnostukset =
        yksilo.getAmmattiKiinnostukset().stream()
            .map(ExtAPIV1Mapper::toAmmattiKiinnostus)
            .collect(Collectors.toSet());
    Set<ExtSuosikkiDto> suosikit =
        yksilo.getSuosikit().stream()
            .map(ExtAPIV1Mapper::toSuosikkiDto)
            .collect(Collectors.toSet());
    Set<ExtPaamaaraDto> paamaarat =
        yksilo.getPaamaarat().stream()
            .map(ExtAPIV1Mapper::toPaamaaraDto)
            .collect(Collectors.toSet());
    return new ExtProfiiliDto(
        yksilo.getId(),
        yksilonOsaamiset,
        osaamisKiinnostukset,
        ammattiKiinnostukset,
        suosikit,
        paamaarat);
  }

  private static ExtPaamaaraDto toPaamaaraDto(Paamaara paamaara) {
    return new ExtPaamaaraDto(
        paamaara.getTyyppi(),
        paamaara.getTyomahdollisuus() != null ? paamaara.getTyomahdollisuus().getId() : null,
        paamaara.getKoulutusmahdollisuus() != null
            ? paamaara.getKoulutusmahdollisuus().getId()
            : null);
  }

  private static ExtSuosikkiDto toSuosikkiDto(YksilonSuosikki yksilonSuosikki) {
    return new ExtSuosikkiDto(
        yksilonSuosikki.getTyomahdollisuus().getId(),
        yksilonSuosikki.getKoulutusmahdollisuus().getId());
  }

  private static ExtAmmattiKiinnostusDto toAmmattiKiinnostus(Ammatti ammatti) {
    return new ExtAmmattiKiinnostusDto(ammatti.getUri(), ammatti.getKoodi());
  }

  private static ExtOsaamisKiinnostusDto toOsaamisKiinnostus(Osaaminen osaaminen) {
    return new ExtOsaamisKiinnostusDto(osaaminen.getUri());
  }

  private static ExtYksilonOsaaminenDto toYksilonOsaaminen(YksilonOsaaminen yksilonOsaaminen) {
    return new ExtYksilonOsaaminenDto(
        yksilonOsaaminen.getLahde().orElse(null), yksilonOsaaminen.getOsaaminen().getUri());
  }
}
