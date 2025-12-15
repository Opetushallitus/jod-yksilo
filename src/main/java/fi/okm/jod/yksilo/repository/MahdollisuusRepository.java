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
import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
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
    // Cache
    // collate() is HQL extension
    final List<MahdollisuusView> mahdollisuudet =
        findByKieli(lang, JpaSort.unsafe(direction, "otsikko"));
    return mahdollisuudet.stream()
        .sorted(createOtsikkoComparator(lang))
        .map(
            m ->
                new MahdollisuusDto(
                    m.getId(),
                    m.getTyyppi(),
                    m.getAmmattiryhma(),
                    m.getAineisto(),
                    m.getKoulutusTyyppi(),
                    m.getMaakunnatList(),
                    m.getKesto()))
        .toList();
  }

  private Comparator<MahdollisuusView> createOtsikkoComparator(Kieli lang) {
    Collator collator = Collator.getInstance(lang.toLocale());
    collator.setStrength(Collator.PRIMARY);
    return Comparator.comparing(MahdollisuusView::getOtsikko, collator);
  }

  List<MahdollisuusView> findByKieli(Kieli kieli, JpaSort unsafe);
}
