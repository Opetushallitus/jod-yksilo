/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import java.util.Collection;
import java.util.Objects;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public final class ApiKeyAuthentication extends AbstractAuthenticationToken {
  private final String apiKey;

  public ApiKeyAuthentication(String apiKey, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.apiKey = apiKey;
    super.setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return null; // no password/credentials other than API key
  }

  @Override
  public Object getPrincipal() {
    return apiKey; // API key as the authenticated principal
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ApiKeyAuthentication aka) {
      return super.equals(obj) && aka.apiKey.equals(this.apiKey);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), apiKey);
  }
}
