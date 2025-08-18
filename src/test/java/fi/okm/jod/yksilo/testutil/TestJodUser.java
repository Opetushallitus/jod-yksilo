/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.testutil;

import fi.okm.jod.yksilo.config.suomifi.Attribute;
import fi.okm.jod.yksilo.domain.FinnishPersonIdentifier;
import fi.okm.jod.yksilo.domain.JodUser;
import java.util.Optional;
import java.util.UUID;

public record TestJodUser(UUID id) implements JodUser {
  private static final FinnishPersonIdentifier personIdentifier =
      FinnishPersonIdentifier.of("010199-9986");

  @Override
  public UUID getId() {
    return id();
  }

  @Override
  public String givenName() {
    return "Test";
  }

  @Override
  public String familyName() {
    return "User";
  }

  @Override
  public String getPersonId() {
    return personIdentifier.asString();
  }

  @Override
  public Optional<String> getAttribute(Attribute attribute) {
    return switch (attribute) {
      case NATIONAL_IDENTIFICATION_NUMBER -> Optional.of(personIdentifier.asString());
      case KOTIKUNTA_KUNTANUMERO -> Optional.of("200");
      default -> Optional.empty();
    };
  }

  public static JodUser of(String uuid) {
    return new TestJodUser(UUID.fromString(uuid));
  }
}
