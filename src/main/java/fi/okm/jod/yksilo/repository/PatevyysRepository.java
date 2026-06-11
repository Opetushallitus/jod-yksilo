/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import static fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi.PATEVYYS;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.OsaamisenLahdeDto;
import fi.okm.jod.yksilo.entity.Patevyys;
import fi.okm.jod.yksilo.entity.Yksilo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatevyysRepository
    extends JpaRepository<Patevyys, UUID>, OsaamisenLahdeRepository<Patevyys> {

  default Optional<Patevyys> findBy(JodUser user, UUID teemaId, UUID id) {
    return findByTeemaYksiloIdAndTeemaIdAndId(user.getId(), teemaId, id);
  }

  List<Patevyys> findByTeemaYksiloIdAndTeemaId(UUID yksiloId, UUID teemaId);

  @EntityGraph(attributePaths = {"teema", "teema.yksilo"})
  Optional<Patevyys> findByTeemaYksiloIdAndTeemaIdAndId(UUID yksiloId, UUID teemaId, UUID id);

  @EntityGraph(attributePaths = {"teema", "teema.yksilo"})
  Optional<Patevyys> findByTeemaYksiloIdAndId(UUID yksiloId, UUID id);

  @Override
  default Optional<Patevyys> findBy(JodUser user, OsaamisenLahdeDto lahde) {
    return lahde.tyyppi() == PATEVYYS
        ? lahde.id().flatMap(lahdeId -> findByTeemaYksiloIdAndId(user.getId(), lahdeId))
        : Optional.empty();
  }

  @EntityGraph(attributePaths = {"teema", "teema.yksilo"})
  List<Patevyys> findByTeemaYksiloIdAndIdIn(UUID yksiloId, Collection<UUID> ids);

  int countByTeemaYksilo(Yksilo yksilo);
}
