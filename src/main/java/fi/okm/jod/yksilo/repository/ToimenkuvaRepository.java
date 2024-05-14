/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.entity.Yksilo;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface ToimenkuvaRepository extends Repository<Toimenkuva, UUID> {

  default Optional<Toimenkuva> findBy(JodUser user, UUID tyopaikkaId, UUID id) {
    return findByTyopaikkaYksiloIdAndTyopaikkaIdAndId(user.getId(), tyopaikkaId, id);
  }

  Optional<Toimenkuva> findByTyopaikkaYksiloIdAndTyopaikkaIdAndId(
      UUID yksiloId, UUID tyopaikkaId, UUID id);

  Optional<Toimenkuva> findByTyopaikkaYksiloAndId(Yksilo yksilo, UUID id);

  default long delete(JodUser user, UUID tyopaikkaId, UUID id) {
    return deleteByTyopaikkaYksiloIdAndTyopaikkaIdAndId(user.getId(), tyopaikkaId, id);
  }

  long deleteByTyopaikkaYksiloIdAndTyopaikkaIdAndId(UUID yksiloId, UUID tyopaikkaId, UUID id);

  Toimenkuva save(Toimenkuva toimenuva);
}
