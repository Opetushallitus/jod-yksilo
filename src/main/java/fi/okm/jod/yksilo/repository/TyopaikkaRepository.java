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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface TyopaikkaRepository extends Repository<Tyopaikka, UUID> {

  Optional<Tyopaikka> findByYksiloIdAndId(UUID yksiloId, UUID id);

  List<Tyopaikka> findByYksiloId(UUID yksiloId);

  void deleteByYksiloIdAndId(UUID yksiloId, UUID id);

  Tyopaikka save(Tyopaikka tyopaikka);
}
