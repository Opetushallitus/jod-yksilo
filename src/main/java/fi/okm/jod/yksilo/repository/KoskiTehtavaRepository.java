/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.domain.KoskiTehtavaTila;
import fi.okm.jod.yksilo.entity.KoskiTehtava;
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

public interface KoskiTehtavaRepository extends JpaRepository<KoskiTehtava, UUID> {

  Optional<KoskiTehtava> findByIdAndYksilo(UUID id, Yksilo yksilo);

  @Transactional
  @Modifying
  @Query("DELETE FROM KoskiTehtava t WHERE t.tila in :tila AND t.luotu < :cutoff")
  int deleteExpired(@Param("tila") Set<KoskiTehtavaTila> tila, @Param("cutoff") Instant cutoff);

  void deleteByYksilo(Yksilo yksilo);
}
