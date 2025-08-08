/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.domain;

import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import java.util.Set;
import java.util.UUID;

/** This OsaamisenLahde has no separate database entity. */
public record MuuOsaaminen(Yksilo yksilo, Set<YksilonOsaaminen> osaamiset)
    implements OsaamisenLahde {
  @Override
  public UUID getId() {
    return null;
  }

  @Override
  public Yksilo getYksilo() {
    return this.yksilo;
  }

  @Override
  public Set<YksilonOsaaminen> getOsaamiset() {
    return this.osaamiset;
  }
}
