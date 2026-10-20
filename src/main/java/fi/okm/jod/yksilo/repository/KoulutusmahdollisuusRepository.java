/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;

public interface KoulutusmahdollisuusRepository extends JpaRepository<Koulutusmahdollisuus, UUID> {
  Page<Koulutusmahdollisuus> findByIdIn(Set<UUID> ids, Pageable pageable);

  /**
   * Returns the osaaminen URIs of the given koulutusmahdollisuus.
   *
   * @param id the koulutusmahdollisuus id
   */
  @NativeQuery(
      """
      SELECT a.arvo
      FROM yksilo.koulutusmahdollisuus_jakauma j
      JOIN yksilo.koulutusmahdollisuus_jakauma_arvot a ON a.koulutusmahdollisuus_jakauma_id = j.id
      WHERE j.koulutusmahdollisuus_id = :id AND j.tyyppi = 'OSAAMINEN'
      """)
  List<String> findOsaamiset(UUID id);

  /**
   * Finds active koulutusmahdollisuudet that share osaamiset with the given ones. The similarity is
   * the Jaccard index of the osaaminen sets, i.e. the number of shared osaamiset divided by the
   * number of osaamiset in either one.
   *
   * @param osaamiset the osaaminen URIs to compare to, must not be empty
   * @param exclude id of a mahdollisuus to leave out of the results
   * @param minYhteiset minimum number of shared osaamiset, capped to the number of given osaamiset
   * @param minSuhde minimum similarity relative to the most similar one, e.g. 0.2 leaves out the
   *     results that are less than a fifth as similar as the best one
   * @param limit maximum number of results
   * @return ids of the most similar koulutusmahdollisuudet, the most similar first
   */
  @NativeQuery(
      """
      SELECT s.id
      FROM (
        SELECT c.id, c.pisteet, MAX(c.pisteet) OVER () AS parhaat_pisteet
        FROM (
          SELECT x.id,
                 CAST(x.yhteiset AS DOUBLE PRECISION) / (
                   :lahdeKoko - x.yhteiset + (SELECT COUNT(*)
                                              FROM yksilo.koulutusmahdollisuus_jakauma_arvot y
                                              WHERE y.koulutusmahdollisuus_jakauma_id = x.jakauma_id)
                 ) AS pisteet
          FROM (
            SELECT j.koulutusmahdollisuus_id AS id, j.id AS jakauma_id, COUNT(*) AS yhteiset
            FROM yksilo.koulutusmahdollisuus_jakauma_arvot a
            JOIN yksilo.koulutusmahdollisuus_jakauma j ON j.id = a.koulutusmahdollisuus_jakauma_id
            JOIN yksilo.koulutusmahdollisuus m ON m.id = j.koulutusmahdollisuus_id
            WHERE a.arvo IN (:osaamiset)
              AND j.tyyppi = 'OSAAMINEN'
              AND j.koulutusmahdollisuus_id <> :exclude
              AND m.aktiivinen = TRUE
            GROUP BY j.koulutusmahdollisuus_id, j.id
          ) x
          WHERE x.yhteiset >= LEAST(:minYhteiset, :lahdeKoko)
        ) c
      ) s
      WHERE s.pisteet >= :minSuhde * s.parhaat_pisteet
      ORDER BY s.pisteet DESC, s.id
      LIMIT :limit
      """)
  List<UUID> findSamankaltaisetIds(
      Collection<String> osaamiset,
      UUID exclude,
      int minYhteiset,
      double minSuhde,
      int lahdeKoko,
      int limit);
}
