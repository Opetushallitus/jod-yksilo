/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.domain;

import fi.okm.jod.yksilo.config.suomifi.Attribute;
import lombok.Getter;

@Getter
public enum PersonIdentifierType {
  FIN(Attribute.NATIONAL_IDENTIFICATION_NUMBER),
  EIDAS(Attribute.PERSON_IDENTIFIER);

  private final Attribute attribute;

  PersonIdentifierType(Attribute attribute) {
    this.attribute = attribute;
  }

  public String asQualifiedIdentifier(String personId) {
    return name() + ":" + personId;
  }
}
