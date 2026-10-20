/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;

public interface TyomahdollisuusRepository extends JpaRepository<Tyomahdollisuus, UUID> {

  /**
   * Returns the osaaminen URIs of the given työmahdollisuus.
   *
   * @param id the työmahdollisuus id
   */
  @NativeQuery(
      """
      SELECT a.arvo
      FROM yksilo.tyomahdollisuus_jakauma j
      JOIN yksilo.tyomahdollisuus_jakauma_arvot a ON a.tyomahdollisuus_jakauma_id = j.id
      WHERE j.tyomahdollisuus_id = :id AND j.tyyppi = 'OSAAMINEN'
      """)
  List<String> findOsaamiset(UUID id);

  /**
   * Finds active työmahdollisuudet that share osaamiset with the given ones. The similarity is the
   * Jaccard index of the osaaminen sets, i.e. the number of shared osaamiset divided by the number
   * of osaamiset in either one.
   *
   * @param osaamiset the osaaminen URIs to compare to, must not be empty
   * @param exclude id of a mahdollisuus to leave out of the results
   * @param minYhteiset minimum number of shared osaamiset, capped to the number of given osaamiset
   * @param minSuhde minimum similarity relative to the most similar one, e.g. 0.2 leaves out the
   *     results that are less than a fifth as similar as the best one
   * @param limit maximum number of results
   * @return ids of the most similar työmahdollisuudet, the most similar first
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
                                              FROM yksilo.tyomahdollisuus_jakauma_arvot y
                                              WHERE y.tyomahdollisuus_jakauma_id = x.jakauma_id)
                 ) AS pisteet
          FROM (
            SELECT j.tyomahdollisuus_id AS id, j.id AS jakauma_id, COUNT(*) AS yhteiset
            FROM yksilo.tyomahdollisuus_jakauma_arvot a
            JOIN yksilo.tyomahdollisuus_jakauma j ON j.id = a.tyomahdollisuus_jakauma_id
            JOIN yksilo.tyomahdollisuus m ON m.id = j.tyomahdollisuus_id
            WHERE a.arvo IN (:osaamiset)
              AND j.tyyppi = 'OSAAMINEN'
              AND j.tyomahdollisuus_id <> :exclude
              AND m.aktiivinen = TRUE
            GROUP BY j.tyomahdollisuus_id, j.id
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
