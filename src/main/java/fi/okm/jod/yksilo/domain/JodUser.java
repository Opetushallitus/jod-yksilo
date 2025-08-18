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
import java.util.Optional;
import java.util.UUID;

public interface JodUser {
  UUID getId();

  String givenName();

  String familyName();

  String getPersonId();

  default Optional<String> getAttribute(Attribute attribute) {
    return Optional.empty();
  }
}
