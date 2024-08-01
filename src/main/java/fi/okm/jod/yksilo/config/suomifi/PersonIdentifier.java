/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.suomifi;

import lombok.Getter;

@Getter
enum PersonIdentifier {
  FIN(Attribute.NATIONAL_IDENTIFICATION_NUMBER),
  EID(Attribute.PERSON_IDENTIFIER);

  private final Attribute attribute;

  PersonIdentifier(Attribute attribute) {
    this.attribute = attribute;
  }
}
