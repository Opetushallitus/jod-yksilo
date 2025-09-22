/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import fi.okm.jod.yksilo.domain.JodUser;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

final class MdcFilter implements Filter {
  @Override
  @SuppressWarnings("try")
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws java.io.IOException, jakarta.servlet.ServletException {
    if (SecurityContextHolder.getContext().getAuthentication() instanceof Authentication auth
        && auth.getPrincipal() instanceof JodUser user) {
      try (var ignored = MDC.putCloseable("userId", user.getId().toString())) {
        chain.doFilter(request, response);
      }
    } else {
      chain.doFilter(request, response);
    }
  }
}
