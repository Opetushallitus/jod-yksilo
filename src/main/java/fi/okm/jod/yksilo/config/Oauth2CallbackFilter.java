/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Saves the frontend path given in the {@code callback} parameter of an OAuth2 authorization
 * request to the session, so that the OAuth2 response endpoint can redirect the user back to it.
 * Only the path is kept to prevent open redirects.
 */
public final class Oauth2CallbackFilter extends OncePerRequestFilter {
  private final String authorizationPath;

  public Oauth2CallbackFilter(String registrationId) {
    this.authorizationPath =
        OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
            + "/"
            + registrationId;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {
    if (request.getRequestURI() instanceof String uri
        && uri.startsWith(request.getContextPath() + authorizationPath)
        && "GET".equals(request.getMethod())
        && request.getParameter("callback") instanceof String cb
        && request.getSession(false) instanceof HttpSession session) {
      session.setAttribute(
          SessionLoginAttribute.CALLBACK_FRONTEND.getKey(), URI.create(cb).normalize().getPath());
    }
    filterChain.doFilter(request, response);
  }
}
