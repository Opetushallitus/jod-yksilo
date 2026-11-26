/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.domain;

import fi.okm.jod.yksilo.config.login.Attribute;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public interface JodUser {

  UUID getId();

  String givenName();

  String familyName();

  String getPersonId();

  String getQualifiedPersonId();

  /** Type of the person identifier, determined by the authentication method. */
  default @Nullable PersonIdentifierType personIdentifierType() {
    return null;
  }

  default Optional<String> getAttribute(Attribute attribute) {
    return Optional.empty();
  }

  default AuthenticationMethod authenticationMethod() {
    return AuthenticationMethod.LOCAL;
  }
}
