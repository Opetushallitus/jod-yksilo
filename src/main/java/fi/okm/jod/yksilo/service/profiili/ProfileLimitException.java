/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import fi.okm.jod.yksilo.service.ServiceValidationException;
import lombok.Getter;

@SuppressWarnings("serial")
public class ProfileLimitException extends ServiceValidationException {
  public enum ProfileItem {
    TOIMINTO,
    PATEVYYS,
    TYOPAIKKA,
    TOIMENKUVA,
    KOULUTUSKOKONAISUUS,
    KOULUTUS,
    OSAAMINEN,
    TAVOITE,
    SUUNNITELMA,
    SUOSIKKI,
    KIINNOSTUKSET,
    JAKOLINKKI
  }

  @Getter private final ProfileItem item;

  public ProfileLimitException(ProfileItem item) {
    super("Profile size limit exceeded");
    this.item = item;
  }
}
