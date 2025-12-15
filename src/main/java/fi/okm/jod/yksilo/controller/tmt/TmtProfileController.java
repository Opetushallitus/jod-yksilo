/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.tmt;

import fi.okm.jod.yksilo.config.SessionLoginAttribute;
import fi.okm.jod.yksilo.config.feature.Feature;
import fi.okm.jod.yksilo.config.feature.FeatureRequired;
import fi.okm.jod.yksilo.config.logging.LogMarker;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.TmtImportDto;
import fi.okm.jod.yksilo.service.tmt.TmtExportService;
import fi.okm.jod.yksilo.service.tmt.TmtImportService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@Tag(name = "/api/integraatiot/tmt")
@Slf4j
@ConditionalOnProperty(name = "jod.tmt.enabled", havingValue = "true")
public class TmtProfileController {

  public static final String ERROR_PARAM = "error";
  private final TmtExportService exportService;
  private final TmtImportService importService;
  private final OAuth2AuthorizedClientRepository authorizedClientRepository;

  /*
   * OAuth2 authorization code flow callback endpoint. This endpoint gets triggered when the OAuth2
   * authorization code flow is completed upon either fail or success.
   */
  @Hidden
  @GetMapping("/oauth2/response/{registrationId:tmt-vienti|tmt-haku}")
  public ResponseEntity<Void> oauth2Response(
      Authentication authentication,
      HttpServletRequest request,
      @AuthenticationPrincipal JodUser jodUser,
      @PathVariable String registrationId,
      @RequestParam(name = "error", required = false) String error,
      @RequestParam(name = "error_description", required = false) String errorDescription) {

    var callback =
        (String)
            request.getSession().getAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey());
    request.getSession().removeAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey());
    if (callback == null || callback.isBlank()) {
      callback = request.getContextPath() + "/";
    }

    String result = "authorization_failed";

    if (error != null) {
      if ("access_denied".equalsIgnoreCase(error)) {
        log.atInfo()
            .addMarker(LogMarker.AUDIT)
            .addKeyValue("userId", jodUser.getId())
            .log("User {} canceled TMT authorization", jodUser.getId());
        result = "authorization_cancelled";
      } else {
        log.warn(
            "TMT OAuth2 authorization failed. Error: {}, description: {}", error, errorDescription);
      }
    }

    var authorizedClient =
        error == null
            ? authorizedClientRepository.loadAuthorizedClient(
                registrationId, authentication, request)
            : null;

    if (authorizedClient == null) {
      return ResponseEntity.status(HttpStatus.SEE_OTHER)
          .location(
              UriComponentsBuilder.fromPath(callback)
                  .queryParam(ERROR_PARAM, result)
                  .build()
                  .toUri())
          .build();
    }

    log.atInfo()
        .addMarker(LogMarker.AUDIT)
        .addKeyValue("userId", jodUser.getId())
        .log("User {} authorized TMT export/import", jodUser.getId());

    return ResponseEntity.status(HttpStatus.SEE_OTHER)
        .location(UriComponentsBuilder.fromPath(callback).queryParam("authorized").build().toUri())
        .build();
  }

  @PostMapping("/api/integraatiot/tmt/vienti")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @FeatureRequired(Feature.TMT_INTEGRATION)
  void export(
      Authentication authentication,
      HttpServletRequest request,
      @RegisteredOAuth2AuthorizedClient("tmt-vienti") OAuth2AuthorizedClient authorizedClient) {
    if (authorizedClient != null && authentication.getPrincipal() instanceof JodUser user) {
      removeAuthorizedClient(authorizedClient, authentication, request);
      exportService.export(user, authorizedClient.getAccessToken());
    } else {
      log.atWarn().addMarker(LogMarker.AUDIT).log("TMT export not authorized");
      throw new IllegalArgumentException("TMT export not authorized");
    }
  }

  @PostMapping("/api/integraatiot/tmt/haku")
  @FeatureRequired(Feature.TMT_INTEGRATION)
  TmtImportDto importProfile(
      Authentication authentication,
      HttpServletRequest request,
      @RegisteredOAuth2AuthorizedClient("tmt-haku") OAuth2AuthorizedClient authorizedClient) {
    if (authorizedClient != null && authentication.getPrincipal() instanceof JodUser user) {
      removeAuthorizedClient(authorizedClient, authentication, request);
      return importService.importProfile(user, authorizedClient.getAccessToken());
    } else {
      log.atWarn().addMarker(LogMarker.AUDIT).log("TMT import not authorized");
      throw new IllegalArgumentException("TMT import not authorized");
    }
  }

  private void removeAuthorizedClient(
      OAuth2AuthorizedClient authorizedClient,
      Authentication authentication,
      HttpServletRequest request) {
    var registrationId = authorizedClient.getClientRegistration().getRegistrationId();
    authorizedClientRepository.removeAuthorizedClient(
        registrationId, authentication, request, null);
  }
}
