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
import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;

@SuppressWarnings("java:S4544")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class JodSaml2Principal extends DefaultSaml2AuthenticatedPrincipal implements JodUser {

  @Serial private static final long serialVersionUID = -9072052257883734365L;
  private final UUID id;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public JodSaml2Principal(
      @JsonProperty("name") String name,
      @JsonProperty("attributes") Map<String, List<Object>> attributes,
      @JsonProperty("sessionIndexes") List<String> sessionIndexes,
      @JsonProperty("registrationId") String relyingPartyRegistrationId,
      @JsonProperty("id") UUID id) {
    super(name, attributes, sessionIndexes);
    super.setRelyingPartyRegistrationId(relyingPartyRegistrationId);
    this.id = requireNonNull(id);
  }

  @Override
  public UUID getId() {
    return id;
  }

  @JsonIgnore
  public String givenName() {
    return getFirstAttribute("http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName");
  }

  @JsonIgnore
  public String familyName() {
    return coalesce(
        getFirstAttribute("urn:oid:2.5.4.4"),
        getFirstAttribute("http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName"));
  }

  @Override
  @JsonProperty("registrationId")
  public String getRelyingPartyRegistrationId() {
    return super.getRelyingPartyRegistrationId();
  }

  @SafeVarargs
  private static <T> T coalesce(T... values) {
    for (T value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }
}
