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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class ApiKeyFilter implements Filter {

  public static final String API_KEY_HEADER_NAME = "Jod-Ext-Api-Key";
  private final String expectedApiKey;

  // Constructor takes the expected API key as parameter
  public ApiKeyFilter(String expectedApiKey) {
    this.expectedApiKey = expectedApiKey;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
      chain.doFilter(request, response);
      return;
    }

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String apiKey = httpRequest.getHeader(API_KEY_HEADER_NAME);

    if (expectedApiKey.equals(apiKey)) {
      Authentication auth = new ApiKeyAuthentication(apiKey, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(auth);
      chain.doFilter(request, response);
    } else {
      httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      httpResponse.getWriter().write("Unauthorized: Invalid or missing API key");
      httpResponse.getWriter().flush();
    }
  }
}
