/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.Osaaminen;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface OsaaminenRepository extends Repository<Osaaminen, Long> {

  Page<Osaaminen> findAll(Pageable page);

  List<Osaaminen> findByUriIn(Collection<String> uri);

  Page<Osaaminen> findByUriIn(Collection<String> uri, Pageable page);
}
