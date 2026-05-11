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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CvTehtavaRepository extends JpaRepository<CvTehtava, UUID> {

  Optional<CvTehtava> findByIdAndYksilo(UUID id, Yksilo yksilo);

  boolean existsByYksiloAndTila(Yksilo yksilo, CvTehtavaTila tila);

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
