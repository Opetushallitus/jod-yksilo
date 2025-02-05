/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.koski;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@ConditionalOnBean(KoskiOAuth2Config.class)
@RestController("/")
@Hidden
public class KoskiOAuth2Controller {

  private final KoskiOAuth2Service koskiOAuth2Service;

  public KoskiOAuth2Controller(KoskiOAuth2Service koskiOAuth2Service) {
    this.koskiOAuth2Service = koskiOAuth2Service;
  }

  @GetMapping("/oauth2/authorize/koski")
  public void redirect(
      HttpServletRequest request, HttpServletResponse response, @RequestParam String callback)
      throws IOException, URISyntaxException {
    var callbackUrl = UrlUtil.getRelativePath(callback);
    request
        .getSession()
        .setAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey(), callbackUrl);
    response.sendRedirect(getAuthorizationUrl(request));
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
   * @param authentication {@link Authentication}
   * @param request {@link HttpServletRequest}
   * @param response {@link HttpServletResponse}
   */
  @GetMapping("/oauth2/response/koski")
  public ResponseEntity<Void> oAuth2DoneCallbackEndpoint(
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response,
      @AuthenticationPrincipal JodUser jodUser)
      throws IOException {
    var authorizedClient = koskiOAuth2Service.getAuthorizedClient(authentication, request);
    if (authorizedClient == null) {
      redirectToFailOrCancelAuthenticationView(request, response, "cancel");
      return ResponseEntity.status(HttpStatus.FOUND).build();
    }
    var jsonData = koskiOAuth2Service.fetchDataFromResourceServer(authorizedClient);
    if (personIdNotMatch(jodUser, jsonData)) {
      koskiOAuth2Service.logout(authentication, request, response);
      log.warn("HETU did NOT match. JOD user != OAuth2 user");
      redirectToFailOrCancelAuthenticationView(request, response, "mismatch");
      return ResponseEntity.status(HttpStatus.FOUND).build();
    }
    var callbackUrl =
        request.getSession().getAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey());
    request.removeAttribute(SessionLoginAttribute.CALLBACK.getKey());
    response.sendRedirect(callbackUrl + "?koski=authorized");
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  private static boolean personIdNotMatch(JodUser jodUser, Object jsonData) {
    var jodUserPersonId = jodUser.getPersonId();
    var jsonDataPersonId = getPersonId(jsonData);
    return !StringUtils.endsWithIgnoreCase(jodUserPersonId, jsonDataPersonId);
  }

  private static String getPersonId(Object jsonData) {
    try {
      return JsonPath.read(jsonData, "$.henkilö.hetu");

    } catch (Exception e) {
      log.debug("HETU was not found in the JSON.");
      return null;
    }
  }

  private static void redirectToFailOrCancelAuthenticationView(
      HttpServletRequest request, HttpServletResponse response, String error) throws IOException {
    var callBackUrl = request.getSession().getAttribute(SessionLoginAttribute.CALLBACK.getKey());
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
