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
import fi.okm.jod.yksilo.entity.Toiminto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatevyysRepository
    extends JpaRepository<Patevyys, UUID>, OsaamisenLahdeRepository<Patevyys> {

  default Optional<Patevyys> findBy(JodUser user, UUID toimintoId, UUID id) {
    return findByToimintoYksiloIdAndToimintoIdAndId(user.getId(), toimintoId, id);
  }

  List<Patevyys> findByToimintoYksiloIdAndToimintoId(UUID yksiloId, UUID toimintoId);

  @EntityGraph(attributePaths = {"toiminto", "toiminto.yksilo"})
  Optional<Patevyys> findByToimintoYksiloIdAndToimintoIdAndId(
      UUID yksiloId, UUID toimintoId, UUID id);

  @EntityGraph(attributePaths = {"toiminto", "toiminto.yksilo"})
  Optional<Patevyys> findByToimintoYksiloIdAndId(UUID yksiloId, UUID id);

  @Override
  default Optional<Patevyys> findBy(JodUser user, OsaamisenLahdeDto lahde) {
    return lahde.tyyppi() == PATEVYYS
        ? findByToimintoYksiloIdAndId(user.getId(), lahde.id())
        : Optional.empty();
  }

  int countByToiminto(Toiminto toiminto);
}
