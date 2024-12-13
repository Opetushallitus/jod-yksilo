/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.ehdotus;

import fi.okm.jod.yksilo.domain.Kieli;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MahdollisuudetService {

  private static final String SQL_UNION_OF_TYO_AND_KOULUTUSMAHDOLLISUUS_IDS =
      """
    SELECT tyomahdollisuus_id AS id, otsikko, 'TYOMAHDOLLISUUS' AS tyyppi
    FROM tyomahdollisuus_kaannos tk
    WHERE kaannos_key = :lang
    UNION
    SELECT koulutusmahdollisuus_id AS id, otsikko, 'KOULUTUSMAHDOLLISUUS' AS tyyppi
    FROM koulutusmahdollisuus_kaannos kk
    WHERE kaannos_key = :lang
    ORDER BY otsikko
    """;

  private static final String SQL_UNION_OF_TYO_AND_KOULUTUSMAHDOLLISUUS_IDS_ASC =
      SQL_UNION_OF_TYO_AND_KOULUTUSMAHDOLLISUUS_IDS + " ASC";

  private static final String SQL_UNION_OF_TYO_AND_KOULUTUSMAHDOLLISUUS_IDS_DESC =
      SQL_UNION_OF_TYO_AND_KOULUTUSMAHDOLLISUUS_IDS + " DESC";

  private final EntityManager entityManager;

  @PostConstruct
  @CacheEvict(value = "mahdollisuusIdsAndTypes", allEntries = true)
  public void clearCacheAtStartup() {
    // This method will clear all entries in the "tyomahdollisuusMetadata" cache at startup
  }

  @Cacheable("mahdollisuusIdsAndTypes")
  public LinkedHashMap<UUID, MahdollisuusTyyppi> fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
      Sort.Direction sort, Kieli lang) {
    // fetch id's and title translation directly from translations
    Query nativeQuery =
        entityManager
            .createNativeQuery(
                sort == Sort.Direction.ASC
                    ? SQL_UNION_OF_TYO_AND_KOULUTUSMAHDOLLISUUS_IDS_ASC
                    : SQL_UNION_OF_TYO_AND_KOULUTUSMAHDOLLISUUS_IDS_DESC,
                TypedResult.class)
            .setParameter("lang", lang.name().toUpperCase());
    @SuppressWarnings("unchecked")
    List<TypedResult> results = nativeQuery.getResultList();
    return results.stream()
        .collect(
            Collectors.toMap(
                result -> result.id, // Key mapper
                result -> MahdollisuusTyyppi.valueOf(result.tyyppi), // Value mapper
                (existing, replacement) -> existing, // Merge function in case of duplicates
                LinkedHashMap::new // Supplier for the LinkedHashMap
                ));
  }

  public enum MahdollisuusTyyppi {
    TYOMAHDOLLISUUS,
    KOULUTUSMAHDOLLISUUS;
  };

  record TypedResult(UUID id, String otsikko, String tyyppi) {}
}
