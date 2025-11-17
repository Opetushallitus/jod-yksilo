/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.ehdotus;

import fi.okm.jod.yksilo.controller.ehdotus.Suggestion;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.MahdollisuusDto;
import fi.okm.jod.yksilo.dto.SuunnitelmaEhdotusDto;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MahdollisuudetService {

  private final KoulutusmahdollisuusRepository koulutusmahdollisuusRepository;

  @PostConstruct
  @CacheEvict(value = "mahdollisuusIdsAndTypes", allEntries = true)
  public void clearCacheAtStartup() {
    // This method will clear all entries in the "tyomahdollisuusMetadata" cache at startup
  }

  @Cacheable("mahdollisuusIdsAndTypes")
  public SequencedMap<UUID, MahdollisuusDto> fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
      Sort.Direction direction, Kieli lang) {

    return koulutusmahdollisuusRepository.findMahdollisuusIds(lang, direction).stream()
        .collect(
            Collectors.toMap(
                MahdollisuusDto::id, // Key mapper
                it -> it, // Value mapper
                (existing, replacement) -> existing, // Merge function in case of duplicates
                LinkedHashMap::new // Supplier for the LinkedHashMap
                ));
  }

  /**
   * Retrieves a list of suggestions for education opportunities (koulutusmahdollisuudet) based on a
   * set of missing competencies (osaamiset).
   *
   * <p>This method performs a query to find opportunities that match the given missing competencies
   * and calculates a match ratio for each suggestion. Only active opportunities are considered.
   *
   * @param missingOsaamiset a set of URIs representing the missing competencies for which
   *     suggestions are to be retrieved
   * @return a list of {@link Suggestion} objects, each containing the details of a matching
   *     opportunity
   */
  public List<SuunnitelmaEhdotusDto> getPolkuVaiheSuggestions(Set<URI> missingOsaamiset) {
    if (missingOsaamiset == null || missingOsaamiset.isEmpty()) {
      return List.of();
    }
    return koulutusmahdollisuusRepository.getPolunVaiheSuggestions(
        missingOsaamiset.stream().map(URI::toString).toList());
  }
}
