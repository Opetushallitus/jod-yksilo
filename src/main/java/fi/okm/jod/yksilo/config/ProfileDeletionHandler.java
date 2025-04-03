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
import fi.okm.jod.yksilo.service.profiili.YksiloService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

@RequiredArgsConstructor
public class ProfileDeletionHandler implements LogoutHandler {
  private final YksiloService yksiloService;

  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    if ("true".equals(request.getParameter("deletion"))) {
      var principal = (JodUser) authentication.getPrincipal();
      yksiloService.delete(principal);
    }
  }
}
