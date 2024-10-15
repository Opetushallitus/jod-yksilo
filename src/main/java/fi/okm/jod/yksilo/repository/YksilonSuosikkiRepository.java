/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.domain.SuosikkiTyyppi;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonSuosikki;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface YksilonSuosikkiRepository extends JpaRepository<YksilonSuosikki, UUID> {
  long deleteByYksiloAndId(Yksilo yksilo, UUID id);

  List<YksilonSuosikki> findByYksilo(Yksilo yksilo);

  List<YksilonSuosikki> findByYksiloAndTyyppi(Yksilo yksilo, SuosikkiTyyppi tyyppi);

  @Query(
      """
      SELECT ys FROM YksilonSuosikki ys WHERE
      ys.yksilo = :yksilo
      AND ys.tyyppi = :tyyppi
      AND ((ys.tyyppi = 'TYOMAHDOLLISUUS' AND ys.tyomahdollisuus.id = :kohdeId)
      OR (ys.tyyppi = 'KOULUTUSMAHDOLLISUUS' AND  ys.koulutusmahdollisuus.id = :kohdeId))
      """)
  Optional<YksilonSuosikki> findBy(Yksilo yksilo, SuosikkiTyyppi tyyppi, UUID kohdeId);
}
