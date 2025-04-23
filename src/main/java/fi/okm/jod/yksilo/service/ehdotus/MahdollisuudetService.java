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
import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.TyomahdollisuusJakaumaTyyppi;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.net.URI;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
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
      SELECT * FROM (SELECT tk.tyomahdollisuus_id AS id, tk.otsikko, ''TYOMAHDOLLISUUS'' AS tyyppi
      FROM tyomahdollisuus_kaannos tk LEFT JOIN tyomahdollisuus t ON tk.tyomahdollisuus_id = t.id
      WHERE tk.kaannos_key = :lang AND t.aktiivinen IS true
      UNION
      SELECT kk.koulutusmahdollisuus_id AS id, kk.otsikko, ''KOULUTUSMAHDOLLISUUS'' AS tyyppi
      FROM koulutusmahdollisuus_kaannos kk LEFT JOIN koulutusmahdollisuus k ON kk.koulutusmahdollisuus_id = k.id
      WHERE kk.kaannos_key = :lang AND k.aktiivinen IS true ) ORDER BY otsikko COLLATE  "{0}" {1}
      """;

  private static final String SQL_OSAAMINEN_SUGGESTIONS_QUERY =
      """
      WITH mahdollisuus AS (
        SELECT
          k.id as id,
        COUNT(a) as totalMatch,
        SIZE(j.arvot) as totalOsaamiset,
        'KOULUTUSMAHDOLLISUUS' as tyyppi
      FROM Koulutusmahdollisuus k
      JOIN k.jakaumat j
      JOIN j.arvot a
      WHERE k.aktiivinen IS true AND j.tyyppi = :koulutusJakaumaTyyppi
      AND a.arvo IN :missingOsaamiset
      GROUP BY k.id, j.id

      UNION ALL

      SELECT
        t.id as id,
        COUNT(a) as totalMatch,
        SIZE(j.arvot) as totalOsaamiset,
        'TYOMAHDOLLISUUS' as tyyppi
      FROM Tyomahdollisuus t
      JOIN t.jakaumat j
      JOIN j.arvot a
      WHERE t.aktiivinen IS true AND j.tyyppi = :tyoJakaumaTyyppi
      AND a.arvo IN :missingOsaamiset
      GROUP BY t.id, j.id
    )
    SELECT NEW fi.okm.jod.yksilo.service.ehdotus.OsaamisetSuggestion(
      m.id,
      CAST(m.totalMatch AS double) / m.totalOsaamiset as matchRatio,
      m.totalMatch,
      CAST(:missingOsaamisetCount AS int),
      m.totalOsaamiset,
      m.tyyppi
    )
    FROM mahdollisuus m
    WHERE :missingOsaamisetCount > 0
    ORDER BY matchRatio DESC
    """;

  private final EntityManager entityManager;

  @PostConstruct
  @CacheEvict(value = "mahdollisuusIdsAndTypes", allEntries = true)
  public void clearCacheAtStartup() {
    // This method will clear all entries in the "tyomahdollisuusMetadata" cache at startup
  }

  @Cacheable("mahdollisuusIdsAndTypes")
  public LinkedHashMap<UUID, MahdollisuusTyyppi> fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
      Sort.Direction sort, Kieli lang) {
    // build sql with order by clause with collate lang and sorting order
    var sql =
        MessageFormat.format(
            SQL_UNION_OF_TYO_AND_KOULUTUSMAHDOLLISUUS_IDS,
            switch (lang) {
              case FI -> "fi-x-icu";
              case SV -> "sv-x-icu";
              case EN -> "en-x-icu";
            },
            sort.name());
    // fetch id's and title translation directly from translations
    Query nativeQuery =
        entityManager
            .createNativeQuery(sql, TypedResult.class)
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

  record TypedResult(UUID id, String otsikko, String tyyppi) {}

  /**
   * Retrieves a list of suggestions for opportunities (koulutusmahdollisuudet and
   * tyomahdollisuudet) based on a set of missing competencies (osaamiset).
   *
   * <p>This method performs a query to find opportunities that match the given missing competencies
   * and calculates a match ratio for each suggestion. Only active opportunities are considered.
   *
   * @param missingOsaamiset a set of URIs representing the missing competencies for which
   *     suggestions are to be retrieved
   * @return a list of {@link Suggestion} objects, each containing the details of a matching
   *     opportunity
   */
  public List<Suggestion> getMahdollisuudetSuggestionsForPolkuVaihe(Set<URI> missingOsaamiset) {
    var missingOsaamisetStrings =
        missingOsaamiset.stream().map(URI::toString).collect(Collectors.toSet());
    var suggestions = executeOsaamisetSuggestionsQuery(missingOsaamisetStrings);
    return suggestions.stream()
        .map(s -> new Suggestion(s.id(), s.matchRatio(), s.tyyppi()))
        .toList();
  }

  private List<OsaamisetSuggestion> executeOsaamisetSuggestionsQuery(
      Set<String> missingOsaamisetStrings) {
    return entityManager
        .createQuery(SQL_OSAAMINEN_SUGGESTIONS_QUERY, OsaamisetSuggestion.class)
        .setParameter("koulutusJakaumaTyyppi", KoulutusmahdollisuusJakaumaTyyppi.OSAAMINEN)
        .setParameter("tyoJakaumaTyyppi", TyomahdollisuusJakaumaTyyppi.OSAAMINEN)
        .setParameter("missingOsaamiset", missingOsaamisetStrings)
        .setParameter("missingOsaamisetCount", missingOsaamisetStrings.size())
        .getResultList();
  }
}
