/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.dto.TyomahdollisuusDto;
import fi.okm.jod.yksilo.dto.profiili.KategoriaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.OsaamisenLahdeDto;
import fi.okm.jod.yksilo.dto.profiili.PatevyysDto;
import fi.okm.jod.yksilo.dto.profiili.ToimenkuvaDto;
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaDto;
import fi.okm.jod.yksilo.dto.profiili.YksilonOsaaminenDto;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKategoria;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.OsaamisenLahde;
import fi.okm.jod.yksilo.entity.Patevyys;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.entity.Tyomahdollisuus;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import java.net.URI;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                .map(o -> URI.create(o.getOsaaminen().getUri()))
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
            entity.getOsaamiset().stream()
                .map(o -> URI.create(o.getOsaaminen().getUri()))
                .collect(Collectors.toUnmodifiableSet()));
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
            entity.getAlkuPvm(),
            entity.getLoppuPvm(),
            entity.getOsaamiset().stream()
                .map(o -> URI.create(o.getOsaaminen().getUri()))
                .collect(Collectors.toUnmodifiableSet()));
  }

  public static KategoriaDto mapKategoria(KoulutusKategoria entity) {
    return entity == null
        ? null
        : new KategoriaDto(entity.getId(), entity.getNimi(), entity.getKuvaus());
  }

  public static OsaaminenDto mapOsaaminen(Osaaminen entity) {
    return entity == null
        ? null
        : new OsaaminenDto(URI.create(entity.getUri()), entity.getNimi(), entity.getKuvaus());
  }

  public static YksilonOsaaminenDto mapYksilonOsaaminen(YksilonOsaaminen entity) {
    return entity == null
        ? null
        : new YksilonOsaaminenDto(
            entity.getId(),
            mapOsaaminen(entity.getOsaaminen()),
            mapOsaamisenLahde(entity.getLahde()));
  }

  private static OsaamisenLahdeDto mapOsaamisenLahde(OsaamisenLahde entity) {
    return entity == null ? null : new OsaamisenLahdeDto(entity.getTyyppi(), entity.getId());
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
                    mapOsaamisenLahde(entity.getLahde())))
        .toList();
  }

  public static TyomahdollisuusDto mapTyomahdollisuus(Tyomahdollisuus entity) {
    return entity == null
        ? null
        : new TyomahdollisuusDto(entity.getId(), entity.getNimi(), entity.getKuvaus());
  }
}
