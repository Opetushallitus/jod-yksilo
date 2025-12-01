/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import static java.util.Objects.requireNonNull;

import fi.okm.jod.yksilo.domain.OsaamisenLahde;
import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.dto.profiili.OsaamisenLahdeDto;
import fi.okm.jod.yksilo.dto.profiili.PatevyysDto;
import fi.okm.jod.yksilo.dto.profiili.PolunSuunnitelmaYhteenvetoDto;
import fi.okm.jod.yksilo.dto.profiili.SuosikkiDto;
import fi.okm.jod.yksilo.dto.profiili.TavoiteDto;
import fi.okm.jod.yksilo.dto.profiili.ToimenkuvaDto;
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaDto;
import fi.okm.jod.yksilo.dto.profiili.YksilonOsaaminenDto;
import fi.okm.jod.yksilo.dto.profiili.suunnitelma.PolunSuunnitelmaDto;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.OsaamisenTunnistusStatus;
import fi.okm.jod.yksilo.entity.Patevyys;
import fi.okm.jod.yksilo.entity.PolunSuunnitelma;
import fi.okm.jod.yksilo.entity.Tavoite;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.entity.YksilonSuosikki;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;

public final class Mapper {

  private Mapper() {}

  public static ToimenkuvaDto mapToimenkuva(Toimenkuva entity) {
    return entity == null
        ? null
        : new ToimenkuvaDto(
            entity.getId(),
            entity.getNimi(),
            entity.getKuvaus(),
            entity.getAlkuPvm(),
            entity.getLoppuPvm(),
            entity.getOsaamiset().stream()
                .map(o -> o.getOsaaminen().getUri())
                .collect(Collectors.toUnmodifiableSet()));
  }

  public static TyopaikkaDto mapTyopaikka(Tyopaikka entity) {
    return entity == null
        ? null
        : new TyopaikkaDto(
            entity.getId(),
            entity.getNimi(),
            entity.getToimenkuvat().stream()
                .map(Mapper::mapToimenkuva)
                .collect(Collectors.toSet()));
  }

  public static KoulutusDto mapKoulutus(Koulutus entity) {
    return entity == null
        ? null
        : new KoulutusDto(
            entity.getId(),
            entity.getNimi(),
            entity.getKuvaus(),
            entity.getAlkuPvm(),
            entity.getLoppuPvm(),
            extractOsaamisetUris(entity),
            isOsaamisetOdottaaTunnistusta(entity.getOsaamisenTunnistusStatus()),
            isOsaamisetTunnistusEpaonnistui(entity.getOsaamisenTunnistusStatus()),
            entity.getOsasuoritukset());
  }

  public static ToimintoDto mapToiminto(Toiminto entity) {
    return entity == null
        ? null
        : new ToimintoDto(
            entity.getId(),
            entity.getNimi(),
            entity.getPatevyydet().stream().map(Mapper::mapPatevyys).collect(Collectors.toSet()));
  }

  public static PatevyysDto mapPatevyys(Patevyys entity) {
    return entity == null
        ? null
        : new PatevyysDto(
            entity.getId(),
            entity.getNimi(),
            entity.getKuvaus(),
            entity.getAlkuPvm(),
            entity.getLoppuPvm(),
            entity.getOsaamiset().stream()
                .map(o -> o.getOsaaminen().getUri())
                .collect(Collectors.toUnmodifiableSet()));
  }

  public static KoulutusKokonaisuusDto mapKoulutusKokonaisuus(KoulutusKokonaisuus entity) {
    return entity == null
        ? null
        : new KoulutusKokonaisuusDto(
            entity.getId(),
            entity.getNimi(),
            entity.getKoulutukset().stream().map(Mapper::mapKoulutus).collect(Collectors.toSet()));
  }

  public static OsaaminenDto mapOsaaminen(Osaaminen entity) {
    requireNonNull(entity);
    return new OsaaminenDto(entity.getUri(), entity.getNimi(), entity.getKuvaus());
  }

  public static YksilonOsaaminenDto mapYksilonOsaaminen(YksilonOsaaminen entity) {
    return entity == null
        ? null
        : new YksilonOsaaminenDto(
            entity.getId(), mapOsaaminen(entity.getOsaaminen()), mapOsaamisenLahde(entity));
  }

  public static TavoiteDto mapTavoite(Tavoite entity) {
    return entity == null
        ? null
        : new TavoiteDto(
            entity.getId(),
            entity.getMahdollisuusTyyppi(),
            entity.getMahdollisuusId(),
            entity.getTavoite(),
            entity.getKuvaus(),
            entity.getLuotu(),
            entity.getSuunnitelmat().stream()
                .map(
                    polunSuunnitelma ->
                        new PolunSuunnitelmaYhteenvetoDto(
                            polunSuunnitelma.getId(),
                            polunSuunnitelma.getNimi(),
                            polunSuunnitelma.getKuvaus(),
                            polunSuunnitelma.getKoulutusmahdollisuusId(),
                            getOsaamiset(polunSuunnitelma)))
                .collect(Collectors.toSet()),
            entity.getOsaamiset());
  }

  private static Set<URI> getOsaamiset(final PolunSuunnitelma entity) {
    Koulutusmahdollisuus koulutusmahdollisuus = entity.getKoulutusmahdollisuus();
    if (koulutusmahdollisuus == null) {
      return entity.getOsaamiset().stream()
          .map(Osaaminen::getUri)
          .collect(Collectors.toUnmodifiableSet());
    }
    return koulutusmahdollisuus.getOsaamiset();
  }

  private static Set<URI> extractOsaamisetUris(Koulutus entity) {
    if (entity.getOsaamiset() == null) {
      return Collections.emptySet();
    }

    return entity.getOsaamiset().stream()
        .map(o -> o.getOsaaminen().getUri())
        .collect(Collectors.toSet());
  }

  private static Boolean isOsaamisetOdottaaTunnistusta(OsaamisenTunnistusStatus status) {
    return status == null ? null : status == OsaamisenTunnistusStatus.WAIT;
  }

  public static PolunSuunnitelmaDto mapPolunSuunnitelma(PolunSuunnitelma entity) {
    if (entity == null) {
      return null;
    }
    Set<URI> osaamiset = getOsaamiset(entity);
    UUID koulutusMahdollisuusId = entity.getKoulutusmahdollisuusId();
    return new PolunSuunnitelmaDto(
        entity.getId(), entity.getNimi(), entity.getKuvaus(), koulutusMahdollisuusId, osaamiset);
  }

  private static Boolean isOsaamisetTunnistusEpaonnistui(OsaamisenTunnistusStatus status) {
    return status == null ? null : status == OsaamisenTunnistusStatus.FAIL;
  }

  private static OsaamisenLahdeDto mapOsaamisenLahde(@NonNull YksilonOsaaminen entity) {
    return new OsaamisenLahdeDto(
        entity.getLahdeTyyppi(), entity.getLahde().map(OsaamisenLahde::getId));
  }

  static <T, U> Function<T, U> cachingMapper(Function<T, U> mapper) {
    final IdentityHashMap<T, U> mappingCache = new IdentityHashMap<>();
    return (T object) -> object == null ? null : mappingCache.computeIfAbsent(object, mapper);
  }

  static List<YksilonOsaaminenDto> mapYksilonOsaaminen(Collection<YksilonOsaaminen> entities) {
    if (entities == null || entities.isEmpty()) {
      return List.of();
    }

    final var mapOsaaminen = cachingMapper(Mapper::mapOsaaminen);

    return entities.stream()
        .map(
            entity ->
                new YksilonOsaaminenDto(
                    entity.getId(),
                    mapOsaaminen.apply(entity.getOsaaminen()),
                    mapOsaamisenLahde(entity)))
        .toList();
  }

  public static SuosikkiDto mapYksilonSuosikki(YksilonSuosikki entity) {
    return entity == null
        ? null
        : new SuosikkiDto(
            entity.getId(), entity.getKohdeId(), entity.getTyyppi(), entity.getLuotu());
  }
}
