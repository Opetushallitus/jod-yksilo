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
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import org.springframework.security.access.AccessDeniedException;

final class SessionDurationFilter implements Filter {
  private final Duration sessionMaxDuration;

  SessionDurationFilter(Duration sessionMaxDuration) {
    this.sessionMaxDuration = sessionMaxDuration;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws java.io.IOException, jakarta.servlet.ServletException {
    if (request instanceof HttpServletRequest req
        && req.getSession(false) instanceof HttpSession session
        && (System.currentTimeMillis() - session.getCreationTime())
            > sessionMaxDuration.toMillis()) {
      req.logout();
      throw new AccessDeniedException("Session maximum duration exceeded");
    }
    chain.doFilter(request, response);
  }
}
