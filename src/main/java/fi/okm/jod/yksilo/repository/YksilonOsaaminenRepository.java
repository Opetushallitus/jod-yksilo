/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

public interface YksilonOsaaminenRepository extends Repository<YksilonOsaaminen, UUID> {

  @EntityGraph(attributePaths = {"osaaminen"})
  List<YksilonOsaaminen> findAllByYksiloId(UUID yksiloId, Sort sort);

  void deleteByYksiloIdAndId(UUID yksiloId, UUID id);

  YksilonOsaaminen save(YksilonOsaaminen yksilonOsaaminen);

  Optional<YksilonOsaaminen> findByYksiloIdAndId(UUID yksiloId, UUID id);

  List<YksilonOsaaminen> saveAll(Iterable<YksilonOsaaminen> list);
}
