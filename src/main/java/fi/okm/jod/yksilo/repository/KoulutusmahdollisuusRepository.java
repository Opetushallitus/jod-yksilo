/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.MahdollisuusDto;
import fi.okm.jod.yksilo.dto.PolunVaiheEhdotusDto;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface KoulutusmahdollisuusRepository extends JpaRepository<Koulutusmahdollisuus, UUID> {

  @Query(
      //  HQL
      //  COUNT counts the number of matching osaamiset (normal SQL aggregation function),
      //  while SIZE returns the total number of osaamiset in the Mahdollisuus.
      //  The j.id in GROUP BY is needed because SIZE() becomes a correlated
      //  subquery that refers to the j.id.
      """
      SELECT NEW fi.okm.jod.yksilo.dto.PolunVaiheEhdotusDto(
        k.id,
        CAST(COUNT(osaamiset) AS double) / SIZE(osaamiset) as matchRatio,
        COUNT(osaamiset) as hits
      )
      FROM Koulutusmahdollisuus k
      JOIN k.jakaumat j
      JOIN j.arvot osaamiset
      WHERE k.aktiivinen = true AND j.tyyppi = 'OSAAMINEN'
      AND osaamiset.arvo IN :missingOsaamiset
      GROUP BY k.id, j.id
      ORDER BY matchRatio DESC
      """)
  List<PolunVaiheEhdotusDto> getPolunVaiheSuggestions(Collection<String> missingOsaamiset);

  default List<MahdollisuusDto> findMahdollisuusIds(Kieli lang, Sort.Direction direction) {
    // collation names are specific to the PostgreSQL database
    var collation =
        switch (lang) {
          case FI -> "fi-x-icu";
          case SV -> "sv-x-icu";
          case EN -> "en-x-icu";
        };
    // collate() is HQL extension
    return findMahdollisuusIdsImpl(
        lang, JpaSort.unsafe(direction, "collate(m.otsikko as `" + collation + "`)"));
  }

  @Query(
      // HQL
      """
      SELECT NEW fi.okm.jod.yksilo.dto.MahdollisuusDto(m.id, m.tyyppi) FROM (
          SELECT t.id AS id, tk.otsikko AS otsikko, 'TYOMAHDOLLISUUS' AS tyyppi
          FROM Tyomahdollisuus t JOIN t.kaannos tk
          WHERE KEY(tk) = :lang AND t.aktiivinen = true
          UNION ALL
          SELECT k.id AS id, kk.otsikko AS otsikko, 'KOULUTUSMAHDOLLISUUS' AS tyyppi
          FROM Koulutusmahdollisuus k JOIN k.kaannos kk
          WHERE KEY(kk) = :lang AND k.aktiivinen = true
      ) m
      """)
  List<MahdollisuusDto> findMahdollisuusIdsImpl(Kieli lang, Sort sort);
}
