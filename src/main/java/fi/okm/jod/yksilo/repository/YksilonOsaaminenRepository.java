/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;

public interface YksilonOsaaminenRepository extends JpaRepository<YksilonOsaaminen, UUID> {

  default List<YksilonOsaaminen> findAllBy(
      UUID yksiloId, @Nullable OsaamisenLahdeTyyppi lahde, Sort sort) {
    return lahde == null
        ? findAllByYksiloId(yksiloId, sort)
        : findAllByYksiloIdAndLahde(yksiloId, lahde, sort);
  }

  @EntityGraph(attributePaths = {"osaaminen"})
  List<YksilonOsaaminen> findAllByYksiloId(UUID yksiloId, Sort sort);

  @EntityGraph(attributePaths = {"osaaminen"})
  List<YksilonOsaaminen> findAllByYksiloIdAndLahde(
      UUID yksiloId, OsaamisenLahdeTyyppi lahde, Sort sort);

  long deleteByYksiloIdAndIdIn(UUID yksiloId, Collection<UUID> ids);

  Optional<YksilonOsaaminen> findByYksiloIdAndId(UUID yksiloId, UUID id);
}
