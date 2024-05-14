/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import fi.okm.jod.yksilo.dto.KoulutusDto;
import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.dto.ToimenkuvaDto;
import fi.okm.jod.yksilo.dto.TyopaikkaDto;
import fi.okm.jod.yksilo.dto.YksilonOsaaminenDto;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import java.net.URI;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Function;

final class Mapper {

  private Mapper() {}

  static ToimenkuvaDto mapToimenkuva(Toimenkuva t) {
    return t == null
        ? null
        : new ToimenkuvaDto(t.getId(), t.getNimi(), t.getKuvaus(), t.getAlkuPvm(), t.getLoppuPvm());
  }

  static TyopaikkaDto mapTyopaikka(Tyopaikka t) {
    return t == null
        ? null
        : new TyopaikkaDto(t.getId(), t.getNimi(), t.getAlkuPvm(), t.getLoppuPvm());
  }

  static KoulutusDto mapKoulutus(Koulutus k) {
    return k == null
        ? null
        : new KoulutusDto(k.getId(), k.getNimi(), k.getKuvaus(), k.getAlkuPvm(), k.getLoppuPvm());
  }

  static OsaaminenDto mapOsaaminen(Osaaminen o) {
    return o == null ? null : new OsaaminenDto(URI.create(o.getUri()), o.getNimi(), o.getKuvaus());
  }

  static YksilonOsaaminenDto mapYksilonOsaaminen(YksilonOsaaminen y) {
    return y == null
        ? null
        : new YksilonOsaaminenDto(
            y.getId(),
            mapOsaaminen(y.getOsaaminen()),
            y.getLahde(),
            mapToimenkuva(y.getToimenkuva()),
            mapKoulutus(y.getKoulutus()));
  }

  static <T, U> Function<T, U> cachingMapper(Function<T, U> mapper) {
    final IdentityHashMap<T, U> mappingCache = new IdentityHashMap<>();
    return (T object) -> object == null ? null : mappingCache.computeIfAbsent(object, mapper);
  }

  static List<YksilonOsaaminenDto> mapYksilonOsaaminen(List<YksilonOsaaminen> items) {

    final var mapOsaaminen = cachingMapper(Mapper::mapOsaaminen);
    final var mapToimenkuva = cachingMapper(Mapper::mapToimenkuva);
    final var mapKoulutus = cachingMapper(Mapper::mapKoulutus);

    return items.stream()
        .map(
            o ->
                new YksilonOsaaminenDto(
                    o.getId(),
                    mapOsaaminen.apply(o.getOsaaminen()),
                    o.getLahde(),
                    mapToimenkuva.apply(o.getToimenkuva()),
                    mapKoulutus.apply(o.getKoulutus())))
        .toList();
  }
}
