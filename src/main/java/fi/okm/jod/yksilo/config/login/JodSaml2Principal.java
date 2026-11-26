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

import fi.okm.jod.yksilo.domain.AuthenticationMethod;
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
  private final PersonIdentifierType identifierType;
  @Getter private final Map<String, List<Object>> attributes;

  JodSaml2Principal(
      UUID id,
      @Nullable PersonIdentifierType identifierType,
      Map<String, List<Object>> attributes) {
    this.id = requireNonNull(id);
    this.attributes = requireNonNull(attributes);
    // Sessions serialized before the identifier type was stored do not have it
    this.identifierType = identifierType != null ? identifierType : inferIdentifierType(attributes);
    if (this.identifierType != PersonIdentifierType.FIN
        && this.identifierType != PersonIdentifierType.EIDAS) {
      throw new IllegalArgumentException("Unsupported identifier type: " + this.identifierType);
    }
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
  public PersonIdentifierType personIdentifierType() {
    return identifierType;
  }

  @Override
  public String getPersonId() {
    return getAttribute(identifierType.getAttribute()).orElse(null);
  }

  @Override
  public String getQualifiedPersonId() {
    return getAttribute(identifierType.getAttribute())
        .map(identifierType::asQualifiedIdentifier)
        .orElse(null);
  }

  @Override
  public Optional<String> getAttribute(Attribute attribute) {
    return Optional.ofNullable(getFirstAttribute(attribute.getUri()));
  }

  @Override
  public AuthenticationMethod authenticationMethod() {
    return AuthenticationMethod.SUOMI_FI;
  }

  @Nullable
  private String getFirstAttribute(String name) {
    return getFirstAttribute(attributes, name);
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private static <A> A getFirstAttribute(Map<String, List<Object>> attributes, String name) {
    List<A> values = (List<A>) attributes.get(name);
    return CollectionUtils.firstElement(values);
  }

  /**
   * Infers the identifier type from the attributes. Suomi.fi sends the eIDAS PersonIdentifier only
   * when a foreign eIDAS authentication method was used, whereas the Finnish national
   * identification number may be present in both cases (eIDAS users linked to a Finnish identity).
   *
   * @see <a href="https://kehittajille.suomi.fi/palvelut/tunnistus/palvelukuvaus">Suomi.fi
   *     palvelukuvaus</a>
   */
  private static PersonIdentifierType inferIdentifierType(Map<String, List<Object>> attributes) {
    return getFirstAttribute(attributes, PersonIdentifierType.EIDAS.getAttribute().getUri()) != null
        ? PersonIdentifierType.EIDAS
        : PersonIdentifierType.FIN;
  }
}
