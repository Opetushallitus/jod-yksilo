/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.koski;

import com.fasterxml.jackson.databind.JsonNode;
import fi.okm.jod.yksilo.config.SessionLoginAttribute;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.util.UrlUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
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
      HttpServletRequest request, HttpServletResponse response, @RequestParam String callback)
      throws IOException, URISyntaxException {
    var callbackModified =
        UrlUtil.getRelativePath(
            UriComponentsBuilder.fromUriString(callback).replaceQuery(null).toUriString());
    request
        .getSession()
        .setAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey(), callbackModified);

    var authorizationUrl = getAuthorizationUrl(request);
    log.debug("Redirect user to {}, callback: {}", authorizationUrl, callbackModified);
    response.sendRedirect(authorizationUrl);
  }

  private String getAuthorizationUrl(HttpServletRequest request) {
    return request.getContextPath()
        + "/oauth2/authorization/"
        + koskiOAuth2Service.getRegistrationId();
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
      @AuthenticationPrincipal JodUser jodUser)
      throws IOException {
    if (jodUser == null) {
      log.trace("User is NOT logged in. Redirect to landing page.");
      response.sendRedirect(request.getContextPath());
      return;
    }
    var authorizedClient = koskiOAuth2Service.getAuthorizedClient(authentication, request);
    if (authorizedClient == null) {
      log.debug("Permission was NOT give by the user id: {}", jodUser.getId());
      redirectToFailOrCancelAuthenticationView(request, response, "cancel");
      return;
    }
    var callbackUrl =
        request
            .getSession()
            .getAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey())
            .toString();
    log.debug("Permission was given by the user id: {}", jodUser.getId());
    JsonNode jsonData;
    try {
      jsonData = koskiOAuth2Service.fetchDataFromResourceServer(authorizedClient);
      if (personIdNotMatch(jodUser, jsonData)) {
        koskiOAuth2Service.logout(authentication, request, response);
        log.warn(
            "HETU did NOT match. JOD user ({}) != OAuth2 user ({})",
            jodUser.givenName(),
            authorizedClient.getPrincipalName());
        redirectToFailOrCancelAuthenticationView(request, response, "mismatch");
        return;
      }
      request.removeAttribute(SessionLoginAttribute.CALLBACK.getKey());
      response.sendRedirect(
          UriComponentsBuilder.fromUriString(callbackUrl)
              .queryParam("koski", "authorized")
              .toUriString());

    } catch (HttpClientErrorException e) {
      response.sendRedirect(
          UriComponentsBuilder.fromUriString(callbackUrl)
              .queryParam("koski", "error")
              .queryParam("code", e.getStatusCode().value())
              .toUriString());
    }
  }

  private static boolean personIdNotMatch(JodUser jodUser, JsonNode jsonData) {
    // if (true) return false; //Bypass for development purpose.
    var jodUserPersonId = jodUser.getPersonId();
    var jsonDataPersonId = getPersonId(jsonData);
    return !StringUtils.endsWithIgnoreCase(jodUserPersonId, jsonDataPersonId);
  }

  private static String getPersonId(JsonNode jsonData) {
    return jsonData.get("henkilö").get("hetu").asText();
  }

  private static void redirectToFailOrCancelAuthenticationView(
      HttpServletRequest request, HttpServletResponse response, String error) throws IOException {
    var callBackUrl =
        request.getSession().getAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey());
    if (callBackUrl != null) {
      response.sendRedirect(callBackUrl + "?error=" + error);
      return;
    }
    response.sendRedirect(getKoulutusUrl(request) + "?error=" + error);
  }

  private static String getKoulutusUrl(HttpServletRequest request) {
    return request.getContextPath() + "/fi/omat-sivuni/osaamiseni/koulutukseni";
  }
}
