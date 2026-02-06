/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.MahdollisuusDto;
import fi.okm.jod.yksilo.dto.SuunnitelmaEhdotusDto;
import fi.okm.jod.yksilo.entity.MahdollisuusView;
import fi.okm.jod.yksilo.entity.MahdollisuusView_;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface MahdollisuusRepository extends JpaRepository<MahdollisuusView, UUID> {

  @Query(
      //  HQL
      //  COUNT counts the number of matching osaamiset (normal SQL aggregation function),
      //  while SIZE returns the total number of osaamiset in the Mahdollisuus.
      //  The j.id in GROUP BY is needed because SIZE() becomes a correlated
      //  subquery that refers to the j.id.
      """
          SELECT NEW fi.okm.jod.yksilo.dto.SuunnitelmaEhdotusDto(
            k.id,
            k.tyyppi as koulutusmahdollisuusTyyppi,
            CAST(k.kesto.mediaani AS double) as kestoMediaani,
            CAST(k.kesto.maksimi AS double) as kestoMaksimi,
            CAST(k.kesto.minimi AS double) as kestoMinimi,
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
  List<SuunnitelmaEhdotusDto> getPolunVaiheSuggestions(Collection<String> missingOsaamiset);

  @Transactional(readOnly = true)
  default List<MahdollisuusDto> findMahdollisuusIds(Kieli lang, Sort.Direction direction) {
    return findByKieli(lang, collate(direction, lang));
  }

  @Transactional(readOnly = true)
  default List<MahdollisuusDto> searchBy(String query, Kieli lang) {
    return searchByImpl(query, lang, collate(Direction.ASC, lang));
  }

  private JpaSort collate(Direction direction, Kieli lang) {
    var collation =
        switch (lang) {
          case FI -> "fi-x-icu";
          case SV -> "sv-x-icu";
          case EN -> "en-x-icu";
        };
    // Note. collate is HQL extension
    return JpaSort.unsafe(
            direction, "collate(trim(" + MahdollisuusView_.OTSIKKO + ") as `" + collation + "`)")
        .and(direction, MahdollisuusView_.id);
  }

  @Query(
      """
          SELECT NEW fi.okm.jod.yksilo.dto.MahdollisuusDto(
                mv.id,
                mv.tyyppi,
                mv.ammattiryhma,
                mv.aineisto,
                mv.koulutusTyyppi,
                mv.maakunnat,
                mv.kesto,
                mv.kestoMinimi,
                mv.kestoMaksimi
          )
          FROM MahdollisuusView mv WHERE mv.kieli = :kieli
          """)
  List<MahdollisuusDto> findByKieli(Kieli kieli, JpaSort sort);

  @Query(
      """
          SELECT NEW fi.okm.jod.yksilo.dto.MahdollisuusDto(
                mv.id,
                mv.tyyppi,
                mv.ammattiryhma,
                mv.aineisto,
                mv.koulutusTyyppi,
                mv.maakunnat,
                mv.kesto,
                mv.kestoMinimi,
                mv.kestoMaksimi
          )
          FROM MahdollisuusView mv
          WHERE mv.kieli = :lang
          AND (mv.tyyppi = 'KOULUTUSMAHDOLLISUUS' AND mv.id IN (
            SELECT k.id FROM Koulutusmahdollisuus k JOIN k.kaannos kk ON KEY(kk) = :lang
            WHERE
                 kk.otsikko ILIKE CONCAT('%', :text, '%')
                 OR kk.kuvaus ILIKE CONCAT('%', :text, '%')
                 OR kk.tiivistelma ILIKE CONCAT('%', :text, '%')
             )
           OR mv.tyyppi = 'TYOMAHDOLLISUUS' AND mv.id IN (
             SELECT t.id FROM Tyomahdollisuus t JOIN t.kaannos tk ON KEY(tk) = :lang
             WHERE
                 tk.otsikko ILIKE CONCAT('%', :text, '%')
                 OR tk.kuvaus ILIKE CONCAT('%', :text, '%')
                 OR tk.tiivistelma ILIKE CONCAT('%', :text, '%')
           )
          )
          """)
  List<MahdollisuusDto> searchByImpl(String text, Kieli lang, JpaSort sort);
}
