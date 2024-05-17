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
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.OsaamisenLahdeDto;
import fi.okm.jod.yksilo.dto.profiili.ToimenkuvaDto;
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaDto;
import fi.okm.jod.yksilo.dto.profiili.YksilonOsaaminenDto;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import java.net.URI;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

final class Mapper {

  private Mapper() {}

  static ToimenkuvaDto mapToimenkuva(Toimenkuva t) {
    return t == null
        ? null
        : new ToimenkuvaDto(
            t.getId(),
            t.getNimi(),
            t.getKuvaus(),
            t.getAlkuPvm(),
            t.getLoppuPvm(),
            t.getOsaamiset().stream()
                .map(o -> URI.create(o.getOsaaminen().getUri()))
                .collect(Collectors.toUnmodifiableSet()));
  }

  static TyopaikkaDto mapTyopaikka(Tyopaikka t) {
    return t == null
        ? null
        : new TyopaikkaDto(
            t.getId(),
            t.getNimi(),
            t.getToimenkuvat().stream().map(Mapper::mapToimenkuva).collect(Collectors.toSet()));
  }

  static KoulutusDto mapKoulutus(Koulutus k) {
    return k == null
        ? null
        : new KoulutusDto(k.getId(), k.getNimi(), k.getKuvaus(), k.getAlkuPvm(), k.getLoppuPvm());
  }

  static OsaaminenDto mapOsaaminen(Osaaminen o) {
    return o == null ? null : new OsaaminenDto(URI.create(o.getUri()), o.getNimi(), o.getKuvaus());
  }

  static YksilonOsaaminenDto mapYksilonOsaaminen(YksilonOsaaminen entity) {
    return entity == null
        ? null
        : new YksilonOsaaminenDto(
            entity.getId(), mapOsaaminen(entity.getOsaaminen()), mapOsaamisenLahde(entity));
  }

  private static OsaamisenLahdeDto mapOsaamisenLahde(YksilonOsaaminen entity) {
    return new OsaamisenLahdeDto(entity.getLahde().getTyyppi(), entity.getLahde().getId());
  }

  static <T, U> Function<T, U> cachingMapper(Function<T, U> mapper) {
    final IdentityHashMap<T, U> mappingCache = new IdentityHashMap<>();
    return (T object) -> object == null ? null : mappingCache.computeIfAbsent(object, mapper);
  }

  static List<YksilonOsaaminenDto> mapYksilonOsaaminen(Collection<YksilonOsaaminen> items) {

    final var mapOsaaminen = cachingMapper(Mapper::mapOsaaminen);

    return items.stream()
        .map(
            o ->
                new YksilonOsaaminenDto(
                    o.getId(), mapOsaaminen.apply(o.getOsaaminen()), mapOsaamisenLahde(o)))
        .toList();
  }
}
