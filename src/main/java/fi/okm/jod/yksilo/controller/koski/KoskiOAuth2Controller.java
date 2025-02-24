/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.koski;

import fi.okm.jod.yksilo.config.SessionLoginAttribute;
import fi.okm.jod.yksilo.config.koski.KoskiOAuth2Config;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.service.koski.KoskiOAuth2Service;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@ConditionalOnBean(KoskiOAuth2Config.class)
@RestController
@RequestMapping("/oauth2")
@Hidden
public class KoskiOAuth2Controller {

  private final KoskiOAuth2Service koskiOAuth2Service;

  public KoskiOAuth2Controller(KoskiOAuth2Service koskiOAuth2Service) {
    this.koskiOAuth2Service = koskiOAuth2Service;
  }

  @GetMapping("/authorize/koski")
  public void redirectToOAuth2AuthorizationUrl(
      HttpServletRequest request, HttpServletResponse response, @RequestParam URI callback)
      throws IOException {
    var callbackPath = callback.getPath();
    request
        .getSession()
        .setAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey(), callbackPath);
    var authorizationUrl = getAuthorizationUrl(request);
    log.debug("Redirect user to {}, callback: {}", authorizationUrl, callbackPath);
    response.sendRedirect(authorizationUrl);
  }

  private String getAuthorizationUrl(HttpServletRequest request) {
    var language = request.getParameter("lang") != null ? request.getParameter("lang") : "fi";
    return UriComponentsBuilder.fromUriString(
            request.getContextPath()
                + "/oauth2/authorization/"
                + koskiOAuth2Service.getRegistrationId())
        .queryParam("locale", language)
        .toUriString();
  }

  /**
   * OAuth2 authorization code flow callback endpoint. This endpoint gets triggered when the OAuth2
   * authorization code flow is completed upon either fail or success. It is defined in
   * application.yml the OAuth2 client registration.
   *
   * @param authentication {@link Authentication} OAuth2 authenticated user.
   * @param request {@link HttpServletRequest}
   * @param response {@link HttpServletResponse}
   * @param jodUser {@link JodUser} JOD logged in user.
   */
  @GetMapping("/response/koski")
  public void oAuth2DoneCallbackEndpoint(
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response,
      @AuthenticationPrincipal JodUser jodUser,
      @RequestParam(name = "error", required = false) String error,
      @RequestParam(name = "error_description", required = false) String errorDescription)
      throws IOException {
    log.trace("Got callback response from Koski Authorization server.");
    if (jodUser == null) {
      log.trace("User is NOT logged in. Redirect to landing page.");
      response.sendRedirect(request.getContextPath() + "/");
      return;
    }
    var callbackUrl = getSavedCallbackUrl(request);
    if (callbackUrl == null) {
      response.sendRedirect(createRedirectUrl(request.getContextPath(), "missingCallback"));
      return;
    }
    if (error != null) {
      if (error.equalsIgnoreCase("access_denied")) {
        handleUserDidNotGivePermission(response, jodUser, callbackUrl);
        return;
      }
      log.error(
          "Koski OAuth2 authorize failed. Error: {}, description: {}", error, errorDescription);
      response.sendRedirect(createRedirectUrl(callbackUrl.toString(), "error"));
      return;
    }
    var authorizedClient = koskiOAuth2Service.getAuthorizedClient(authentication, request);
    if (authorizedClient == null) {
      handleUserDidNotGivePermission(response, jodUser, callbackUrl);
      return;
    }
    log.debug("Permission was given by the user id: {}", jodUser.getId());
    request.getSession().removeAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey());
    response.sendRedirect(createRedirectUrl(callbackUrl.toString(), "authorized"));
  }

  private static void handleUserDidNotGivePermission(
      HttpServletResponse response, JodUser jodUser, Object callbackUrl) throws IOException {
    log.debug("Permission was NOT give by the user id: {}", jodUser.getId());
    response.sendRedirect(createRedirectUrl(callbackUrl.toString(), "cancel"));
  }

  private static Object getSavedCallbackUrl(HttpServletRequest request) {
    return request.getSession().getAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey());
  }

  private static String createRedirectUrl(String callbackUrl, Object value) {
    return UriComponentsBuilder.fromUriString(callbackUrl).queryParam("koski", value).toUriString();
  }
}
