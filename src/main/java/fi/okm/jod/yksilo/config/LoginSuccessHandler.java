/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@RequiredArgsConstructor
@Slf4j
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final RedirectStrategy redirectStrategy;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    String callback = null;
    if (request.getSession(false) instanceof HttpSession s) {
      callback = (String) s.getAttribute(SessionLoginAttribute.CALLBACK.getKey());

      for (SessionLoginAttribute attr : SessionLoginAttribute.values()) {
        s.removeAttribute(attr.getKey());
      }
      s.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

    try {
      redirectStrategy.sendRedirect(request, response, callback != null ? callback : "/");
    } catch (IOException e) {
      log.error("Failed to redirect", e);
    }
  }
}
