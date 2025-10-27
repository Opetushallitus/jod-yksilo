/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.Tavoite;
import fi.okm.jod.yksilo.entity.Yksilo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TavoiteRepository extends JpaRepository<Tavoite, UUID> {

  List<Tavoite> findAllByYksilo(Yksilo yksilo);

  int deleteByYksiloAndId(Yksilo yksilo, UUID id);

  Optional<Tavoite> findByYksiloAndId(Yksilo yksilo, UUID id);

  Optional<Tavoite> findByYksiloIdAndId(UUID yksiloId, UUID id);

  int countByYksilo(Yksilo yksilo);
}
