/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
import java.util.Set;
import java.util.UUID;

public sealed interface OsaamisenLahde permits Toimenkuva, Koulutus, Patevyys {

  UUID getId();

  Yksilo getYksilo();

  Set<YksilonOsaaminen> getOsaamiset();

  default OsaamisenLahdeTyyppi getTyyppi() {
    return switch (this) {
      case Toimenkuva ignored -> OsaamisenLahdeTyyppi.TOIMENKUVA;
      case Koulutus ignored -> OsaamisenLahdeTyyppi.KOULUTUS;
      case Patevyys ignored -> OsaamisenLahdeTyyppi.PATEVYYS;
    };
  }
}
