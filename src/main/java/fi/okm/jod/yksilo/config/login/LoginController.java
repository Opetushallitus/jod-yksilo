/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.login;

import static fi.okm.jod.yksilo.config.SessionLoginAttribute.CALLBACK;
import static fi.okm.jod.yksilo.config.SessionLoginAttribute.LANG;

import fi.okm.jod.yksilo.domain.Kieli;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@ConditionalOnBean(LoginConfig.class)
@Hidden
class LoginController {
  @Value("${jod.saml2.relying-party.registration-id}")
  private String registrationId;

  private final MpassidOidcUserConverter mpassidOidcUserService;

  public static final String REDIRECT_URI = "/saml2/authenticate/{registrationId}";

  LoginController(ObjectProvider<MpassidOidcUserConverter> mpassidOidcUserService) {
    this.mpassidOidcUserService = mpassidOidcUserService.getIfAvailable();
  }

  @GetMapping("/login")
  void login(
      @RequestParam(required = false) Kieli lang,
      @RequestParam(required = false) URI callback,
      @RequestParam(required = false) String method,
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication)
      throws IOException {

    if (lang != null) {
      request.getSession().setAttribute(LANG.getKey(), lang.toString());
    }
    if (callback != null && callback.getPath() != null) {
      request.getSession().setAttribute(CALLBACK.getKey(), callback.normalize().getPath());
    }

    if ("mpassid".equals(method) && mpassidOidcUserService != null) {
      response.sendRedirect(request.getContextPath() + "/oauth2/authorization/mpassid");
    } else {
      response.sendRedirect(
          request.getContextPath() + REDIRECT_URI.replace("{registrationId}", registrationId));
    }
  }
}
