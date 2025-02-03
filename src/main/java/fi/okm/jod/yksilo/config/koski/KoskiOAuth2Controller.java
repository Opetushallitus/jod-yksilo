/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.koski;

import fi.okm.jod.yksilo.config.SessionLoginAttribute;
import fi.okm.jod.yksilo.service.KoskiService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Profile("!test")
@ConditionalOnBean(KoskiOAuth2Config.class)
@RestController("/")
@Hidden
public class KoskiOAuth2Controller {

  private final KoskiOAuth2Service koskiOAuth2Service;
  private final KoskiService koskiService;

  public KoskiOAuth2Controller(KoskiOAuth2Service koskiOAuth2Service, KoskiService koskiService) {
    this.koskiOAuth2Service = koskiOAuth2Service;
    this.koskiService = koskiService;
  }

  @GetMapping("/koski")
  public void redirect(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    var referer = request.getHeader("referer");
    request.setAttribute(SessionLoginAttribute.CALLBACK.getKey(), referer);
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
  public ResponseEntity<?> oAuth2DoneCallbackEndpoint(
      Authentication authentication, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    var auth2AuthorizedClient = koskiOAuth2Service.getAuthorizedClient(authentication, request);
    if (auth2AuthorizedClient == null) {
      redirectToFailOrCancelAuthenticationView(request, response);
      return ResponseEntity.status(HttpStatus.FOUND).build();
    }
    var jsonData = koskiOAuth2Service.fetchDataFromResourceServer(auth2AuthorizedClient);
    var koskiData = koskiService.getKoulutusData(jsonData);
    return ResponseEntity.ok().body(koskiData);
  }

  private static void redirectToFailOrCancelAuthenticationView(
      HttpServletRequest request, HttpServletResponse response) throws IOException {
    var callBackUrl = request.getAttribute(SessionLoginAttribute.CALLBACK.getKey());
    if (callBackUrl != null) {
      response.sendRedirect(callBackUrl.toString());
    } else {
      response.sendRedirect(getKoulutusUrl(request));
    }
  }

  private static String getKoulutusUrl(HttpServletRequest request) {
    return request.getContextPath() + "/fi/omat-sivuni/osaamiseni/koulutukseni";
  }
}
