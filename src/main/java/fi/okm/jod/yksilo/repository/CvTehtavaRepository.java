/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.domain.CvTehtavaTila;
import fi.okm.jod.yksilo.entity.CvTehtava;
import fi.okm.jod.yksilo.entity.Yksilo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CvTehtavaRepository extends JpaRepository<CvTehtava, UUID> {

  record CountByTila(CvTehtavaTila tila, long lukumaara) {}

  Optional<CvTehtava> findByIdAndYksilo(UUID id, Yksilo yksilo);

  @Query(
      "SELECT t FROM CvTehtava t WHERE t.yksilo = :yksilo AND t.kieli = :kieli "
          + "AND t.sisaltoHash = :sisaltoHash AND t.tila = fi.okm.jod.yksilo.domain.CvTehtavaTila.VALMIS "
          + "AND t.tulos IS NOT NULL ORDER BY t.luotu DESC")
  List<CvTehtava> findCompletedByContent(
      @Param("yksilo") Yksilo yksilo,
      @Param("kieli") fi.okm.jod.yksilo.domain.Kieli kieli,
      @Param("sisaltoHash") String sisaltoHash);

  @Query(
      "SELECT t.tila, count(*) AS lukumaara FROM CvTehtava t WHERE t.yksilo = :yksilo AND t.luotu >= :luotu GROUP BY t.tila")
  List<CountByTila> countByYksiloAndTila(Yksilo yksilo, Instant luotu);

  @Transactional
  @Modifying
  @Query("UPDATE CvTehtava t SET t.tila = :tila WHERE t.id = :id")
  void updateTila(@Param("id") UUID id, @Param("tila") CvTehtavaTila tila);

  @Transactional
  @Modifying
  @Query("DELETE FROM CvTehtava t WHERE t.tila in :tila AND t.luotu < :cutoff")
  int deleteExpired(@Param("tila") Set<CvTehtavaTila> tila, @Param("cutoff") Instant cutoff);

  @Transactional
  @Modifying
  @Query(
      "UPDATE CvTehtava t SET t.tila = 'EPAONNISTUNUT', t.muokattu = CURRENT_TIMESTAMP WHERE t.tila = :tila AND t.luotu < :cutoff")
  int failExpired(@Param("tila") CvTehtavaTila tila, @Param("cutoff") Instant cutoff);

  void deleteByYksilo(Yksilo yksilo);

  Optional<CvTehtava> findByIdAndYksiloId(UUID id, UUID yksiloId);
}
