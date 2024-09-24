/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.Yksilo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TyopaikkaRepository extends JpaRepository<Tyopaikka, UUID> {

  Optional<Tyopaikka> findByYksiloIdAndId(UUID yksiloId, UUID id);

  List<Tyopaikka> findByYksiloId(UUID yksiloId);

  long countByYksilo(Yksilo yksilo);

  @Modifying(flushAutomatically = true)
  @Query(
      "DELETE FROM Tyopaikka t WHERE t.id = :id AND t.yksilo.id = :yksiloId AND t.toimenkuvat IS EMPTY")
  void deleteEmpty(UUID yksiloId, UUID id);
}
