/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import static fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi.TOIMENKUVA;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.OsaamisenLahdeDto;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.entity.Yksilo;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToimenkuvaRepository
    extends JpaRepository<Toimenkuva, UUID>, OsaamisenLahdeRepository<Toimenkuva> {

  default Optional<Toimenkuva> findBy(JodUser user, UUID tyopaikkaId, UUID id) {
    return findByTyopaikkaYksiloIdAndTyopaikkaIdAndId(user.getId(), tyopaikkaId, id);
  }

  @EntityGraph(attributePaths = {"tyopaikka", "tyopaikka.yksilo"})
  Optional<Toimenkuva> findByTyopaikkaYksiloIdAndTyopaikkaIdAndId(
      UUID yksiloId, UUID tyopaikkaId, UUID id);

  @EntityGraph(attributePaths = {"tyopaikka", "tyopaikka.yksilo"})
  Optional<Toimenkuva> findByTyopaikkaYksiloIdAndId(UUID yksiloId, UUID id);

  @Override
  default Optional<Toimenkuva> findBy(JodUser user, OsaamisenLahdeDto lahde) {
    return lahde.tyyppi() == TOIMENKUVA
        ? lahde.id().flatMap(lahdeId -> findByTyopaikkaYksiloIdAndId(user.getId(), lahdeId))
        : Optional.empty();
  }

  int countByTyopaikkaYksilo(Yksilo yksilo);
}
