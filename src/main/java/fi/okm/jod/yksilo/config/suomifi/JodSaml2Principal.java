/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.suomifi;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import fi.okm.jod.yksilo.domain.JodUser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Setter;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;

@SuppressWarnings({"java:S4544", "serial"})
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class JodSaml2Principal extends DefaultSaml2AuthenticatedPrincipal implements JodUser {

  private final UUID id;
  @Setter private boolean tervetuloapolku;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public JodSaml2Principal(
      @JsonProperty("name") String name,
      @JsonProperty("attributes") Map<String, List<Object>> attributes,
      @JsonProperty("sessionIndexes") List<String> sessionIndexes,
      @JsonProperty("registrationId") String relyingPartyRegistrationId,
      @JsonProperty("id") UUID id,
      @JsonProperty("tervetuloapolku") boolean tervetuloapolku) {
    super(name, attributes, sessionIndexes);
    super.setRelyingPartyRegistrationId(relyingPartyRegistrationId);
    this.id = requireNonNull(id);
    this.tervetuloapolku = tervetuloapolku;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @JsonIgnore
  @Override
  public String givenName() {
    return getAttribute(Attribute.GIVEN_NAME)
        .or(() -> getAttribute(Attribute.FIRST_NAME))
        .orElse(null);
  }

  @JsonIgnore
  @Override
  public String familyName() {
    return getAttribute(Attribute.SN).or(() -> getAttribute(Attribute.FAMILY_NAME)).orElse(null);
  }

  @JsonIgnore
  @Override
  public String getPersonId() {
    return getAttribute(PersonIdentifier.FIN.getAttribute())
        .orElse(getAttribute(PersonIdentifier.EIDAS.getAttribute()).orElse(null));
  }

  @Override
  public boolean getTervetuloapolku() {
    return tervetuloapolku;
  }

  @JsonIgnore
  public Optional<String> getAttribute(Attribute attribute) {
    return Optional.ofNullable(getFirstAttribute(attribute.getUri()));
  }

  @Override
  @JsonProperty("registrationId")
  public String getRelyingPartyRegistrationId() {
    return super.getRelyingPartyRegistrationId();
  }

  // for SpotBugs, intentionally using the inherited equals and hashCode
  @Override
  public boolean equals(Object object) {
    return super.equals(object);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
