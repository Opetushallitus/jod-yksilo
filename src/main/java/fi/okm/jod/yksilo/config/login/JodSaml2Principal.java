/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.login;

import static java.util.Objects.requireNonNull;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.PersonIdentifierType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.util.CollectionUtils;

public class JodSaml2Principal implements AuthenticatedPrincipal, JodUser {

  private final UUID id;
  @Getter private final Map<String, List<Object>> attributes;

  JodSaml2Principal(UUID id, Map<String, List<Object>> attributes) {
    this.id = requireNonNull(id);
    this.attributes = requireNonNull(attributes);
  }

  @Override
  public @NonNull String getName() {
    return getQualifiedPersonId();
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public String givenName() {
    return getAttribute(Attribute.GIVEN_NAME)
        .or(() -> getAttribute(Attribute.FIRST_NAME))
        .orElse(null);
  }

  @Override
  public String familyName() {
    return getAttribute(Attribute.SN).or(() -> getAttribute(Attribute.FAMILY_NAME)).orElse(null);
  }

  @Override
  public String getPersonId() {
    return getAttribute(PersonIdentifierType.FIN.getAttribute())
        .orElse(getAttribute(PersonIdentifierType.EIDAS.getAttribute()).orElse(null));
  }

  @Override
  public String getQualifiedPersonId() {
    return getAttribute(PersonIdentifierType.FIN.getAttribute())
        .map(PersonIdentifierType.FIN::asQualifiedIdentifier)
        .orElse(
            getAttribute(PersonIdentifierType.EIDAS.getAttribute())
                .map(PersonIdentifierType.EIDAS::asQualifiedIdentifier)
                .orElse(null));
  }

  public Optional<String> getAttribute(Attribute attribute) {
    return Optional.ofNullable(getFirstAttribute(attribute.getUri()));
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <A> A getFirstAttribute(String name) {
    List<A> values = (List<A>) attributes.get(name);
    return CollectionUtils.firstElement(values);
  }
}
