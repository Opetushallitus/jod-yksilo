/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.domain;

import com.google.common.base.CaseFormat;

public enum KoulutusmahdollisuusJakaumaTyyppi {
  OSAAMINEN,
  KOULUTUSALA,
  MAKSULLISUUS,
  OPETUSTAPA,
  AIKA,
  KUNTA,
  MAAKUNTA;

  private final String propertyName;

  @Override
  public String toString() {
    return propertyName;
  }

  KoulutusmahdollisuusJakaumaTyyppi() {
    this.propertyName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());
  }
}
