/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.mocklogin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fi.okm.jod.yksilo.domain.JodUser;
import java.io.Serial;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@EqualsAndHashCode(of = "username")
@JsonDeserialize
public class MockJodUserImpl implements UserDetails, JodUser {

  @Serial private static final long serialVersionUID = 8118273720670747702L;

  private final String username;
  @Getter private final UUID id;

  @JsonCreator
  public MockJodUserImpl(@JsonProperty("username") String username, @JsonProperty("id") UUID id) {
    this.username = username;
    this.id = id;
  }

  public static final SimpleGrantedAuthority ROLE_USER = new SimpleGrantedAuthority("ROLE_USER");

  @Override
  @JsonIgnore
  public String getPassword() {
    return "{noop}password";
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  @JsonIgnore
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  @JsonIgnore
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  @JsonIgnore
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  @JsonIgnore
  public boolean isEnabled() {
    return true;
  }

  @Override
  @JsonIgnore
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Set.of(ROLE_USER);
  }

  @Override
  public String givenName() {
    return "Mock" + id.hashCode();
  }

  @Override
  public String familyName() {
    return "User";
  }
}
