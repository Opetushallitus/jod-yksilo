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
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyFilter extends OncePerRequestFilter {

  public static final String API_KEY_HEADER_NAME = "Jod-Ext-Api-Key";
  private final String expectedApiKey;

  // Constructor takes the expected API key as parameter
  public ApiKeyFilter(String expectedApiKey) {
    this.expectedApiKey = Objects.requireNonNull(expectedApiKey, "Api key should not be null");
  }

  @Override
  public void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String apiKey = request.getHeader(API_KEY_HEADER_NAME);
    if (expectedApiKey.equals(apiKey)) {
      Authentication auth =
          new ApiKeyAuthentication(
              apiKey, List.of(new SimpleGrantedAuthority(JodRole.EXTERNAL_API.name())));
      SecurityContextHolder.getContext().setAuthentication(auth);
    }
    chain.doFilter(request, response);
  }
}
