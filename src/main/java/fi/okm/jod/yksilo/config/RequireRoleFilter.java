/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

/** Enforce authenticated user with specified role. */
public final class RequireRoleFilter implements Filter {
  private final String role;

  public RequireRoleFilter(JodRole role) {
    this.role = "ROLE_" + role.name();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    var securityContext = SecurityContextHolder.getContext();
    var authentication = securityContext.getAuthentication();
    if (authentication == null
        || authentication.getAuthorities().stream().noneMatch(a -> role.equals(a.getAuthority()))) {
      throw new InsufficientAuthenticationException("User does not have required role: " + role);
    }
    chain.doFilter(request, response);
  }
}
