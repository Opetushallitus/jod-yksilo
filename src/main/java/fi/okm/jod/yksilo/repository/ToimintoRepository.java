/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.entity.Yksilo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToimintoRepository extends JpaRepository<Toiminto, UUID> {
  Optional<Toiminto> findByYksiloIdAndId(UUID yksiloId, UUID id);

  List<Toiminto> findByYksiloIdAndIdIn(UUID yksiloId, Set<UUID> ids);

  List<Toiminto> findByYksiloId(UUID yksiloId);

  long countByYksilo(Yksilo yksilo);

  long deleteByYksiloIdAndIdIn(UUID yksiloId, Collection<UUID> ids);
}
