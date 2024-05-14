/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.Yksilo;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface YksiloRepository extends Repository<Yksilo, UUID> {

  Yksilo getByTunnus(String tunnus);

  Yksilo getReferenceById(UUID id);
}
