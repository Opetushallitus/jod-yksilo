/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.suomifi;

import static fi.okm.jod.yksilo.config.SessionLoginAttribute.CALLBACK;
import static fi.okm.jod.yksilo.config.SessionLoginAttribute.LANG;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@ConditionalOnBean(Saml2LoginConfig.class)
@Hidden
class LoginController {
  @Value("${jod.saml2.relying-party.registration-id}")
  private String registrationId;

  public static final String REDIRECT_URI = "/saml2/authenticate/{registrationId}";

  @GetMapping("/login")
  void login(
      @RequestParam(required = false) String lang,
      @RequestParam(required = false) URI callback,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {

    if (lang != null && !lang.isEmpty()) {
      request.getSession().setAttribute(LANG.getKey(), lang);
    }
    if (callback != null && callback.getPath() != null) {
      request.getSession().setAttribute(CALLBACK.getKey(), callback.getPath());
    }

    response.sendRedirect(
        request.getContextPath() + REDIRECT_URI.replace("{registrationId}", registrationId));
  }
}
