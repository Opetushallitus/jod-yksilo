/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.suomifi;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

@Controller
@ConditionalOnBean(Saml2LoginConfig.class)
@Hidden
class LoginController {

  public static final String REDIRECT_URI = "/saml2/authenticate/jodsuomifi";

  @GetMapping("/login")
  void login(@RequestParam(required = false) String lang, HttpServletResponse response)
      throws IOException {
    var redirectUri = REDIRECT_URI;
    if (lang != null && !lang.isEmpty()) {
      redirectUri += "?lang=" + UriUtils.encodeQueryParam(lang, StandardCharsets.UTF_8);
    }
    response.sendRedirect(redirectUri);
  }
}
