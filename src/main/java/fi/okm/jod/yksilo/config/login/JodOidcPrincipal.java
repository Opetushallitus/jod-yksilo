/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.login;

import static fi.okm.jod.yksilo.config.login.Attribute.OPPIJANUMERO_CLAIM;
import static java.util.Objects.requireNonNull;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.PersonIdentifierType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class JodOidcPrincipal implements OidcUser, JodUser {

  private static final List<GrantedAuthority> AUTHORITIES =
      List.of(new SimpleGrantedAuthority("ROLE_USER"));

  private final UUID id;
  private final OidcIdToken idToken;

  JodOidcPrincipal(UUID id, OidcIdToken idToken) {
    this.id = requireNonNull(id);
    this.idToken = idToken;
  }

  @Override
  public @NonNull String getName() {
    return getQualifiedPersonId();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public String getOppijanumero() {
    return this.getClaimAsString(OPPIJANUMERO_CLAIM.getUri());
  }

  @Override
  @Nullable
  public String givenName() {
    return this.getGivenName();
  }

  @Override
  @Nullable
  public String familyName() {
    return this.getFamilyName();
  }

  @Override
  public String getPersonId() {
    return getOppijanumero();
  }

  @Override
  public String getQualifiedPersonId() {
    return PersonIdentifierType.ONR.asQualifiedIdentifier(getOppijanumero());
  }

  // OidcUser implementation

  @Override
  public Map<String, Object> getClaims() {
    return idToken.getClaims();
  }

  @Override
  @Nullable
  public OidcUserInfo getUserInfo() {
    return null;
  }

  @Override
  public OidcIdToken getIdToken() {
    return idToken;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return idToken.getClaims();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return AUTHORITIES;
  }
}
