/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.dto.SamankaltainenMahdollisuusDto;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds mahdollisuudet that are similar to a given mahdollisuus based on shared osaamiset. The
 * similar mahdollisuudet can be of either type regardless of the type of the given one, as both
 * types use the same osaaminen URIs.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SamankaltaisetMahdollisuudetService {
  private static final int MAX_SAMANKALTAISET = 5;
  private static final int MIN_YHTEISET_OSAAMISET = 3;

  // Leaves out weak matches that follow much stronger ones, as the similarity scores differ between
  // the type combinations, the limit is relative to the best match instead of being absolute.
  private static final double MIN_SUHDE_PARHAASEEN = 0.2;

  private final TyomahdollisuusRepository tyomahdollisuudet;
  private final KoulutusmahdollisuusRepository koulutusmahdollisuudet;

  /**
   * Returns active mahdollisuudet of the requested type that share the most osaamiset with the
   * given mahdollisuus, the most similar first.
   *
   * @param lahdeTyyppi type of the mahdollisuus to find similar ones for
   * @param id id of the mahdollisuus to find similar ones for
   * @param kohdeTyyppi type of the mahdollisuudet to return
   * @throws NotFoundException if the mahdollisuus does not exist
   */
  public List<SamankaltainenMahdollisuusDto> find(
      MahdollisuusTyyppi lahdeTyyppi, UUID id, MahdollisuusTyyppi kohdeTyyppi) {
    var osaamiset = findOsaamiset(lahdeTyyppi, id);
    if (osaamiset.isEmpty()) {
      return List.of();
    }
    return switch (kohdeTyyppi) {
      case TYOMAHDOLLISUUS ->
          findSimilar(
              osaamiset,
              id,
              tyomahdollisuudet::findSamankaltaisetIds,
              tyomahdollisuudet::findAllById,
              Tyomahdollisuus::getId,
              t -> new SamankaltainenMahdollisuusDto(t.getId(), t.getOtsikko()));
      case KOULUTUSMAHDOLLISUUS ->
          findSimilar(
              osaamiset,
              id,
              koulutusmahdollisuudet::findSamankaltaisetIds,
              koulutusmahdollisuudet::findAllById,
              Koulutusmahdollisuus::getId,
              k -> new SamankaltainenMahdollisuusDto(k.getId(), k.getOtsikko()));
    };
  }

  private List<String> findOsaamiset(MahdollisuusTyyppi tyyppi, UUID id) {
    return switch (tyyppi) {
      case TYOMAHDOLLISUUS -> {
        if (!tyomahdollisuudet.existsById(id)) {
          throw new NotFoundException("Unknown tyomahdollisuus");
        }
        yield tyomahdollisuudet.findOsaamiset(id);
      }
      case KOULUTUSMAHDOLLISUUS -> {
        if (!koulutusmahdollisuudet.existsById(id)) {
          throw new NotFoundException("Unknown Koulutusmahdollisuus");
        }
        yield koulutusmahdollisuudet.findOsaamiset(id);
      }
    };
  }

  @FunctionalInterface
  private interface IdFinder {
    List<UUID> find(
        List<String> osaamiset,
        UUID exclude,
        int minYhteiset,
        double minSuhde,
        int lahdeKoko,
        int limit);
  }

  private static <T> List<SamankaltainenMahdollisuusDto> findSimilar(
      List<String> osaamiset,
      UUID id,
      IdFinder idFinder,
      Function<List<UUID>, List<T>> entityLoader,
      Function<T, UUID> idOf,
      Function<T, SamankaltainenMahdollisuusDto> mapper) {
    var ids =
        idFinder.find(
            osaamiset,
            id,
            MIN_YHTEISET_OSAAMISET,
            MIN_SUHDE_PARHAASEEN,
            osaamiset.size(),
            MAX_SAMANKALTAISET);
    var byId = entityLoader.apply(ids).stream().collect(Collectors.toMap(idOf, e -> e));
    return ids.stream().map(byId::get).filter(Objects::nonNull).map(mapper).toList();
  }
}
