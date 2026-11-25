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
import fi.okm.jod.yksilo.config.feature.Feature;
import fi.okm.jod.yksilo.config.feature.FeatureRequired;
import fi.okm.jod.yksilo.config.koski.KoskiOauth2Config;
import fi.okm.jod.yksilo.config.logging.LogMarker;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaDto;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaSaveDto;
import fi.okm.jod.yksilo.service.koski.KoskiService;
import fi.okm.jod.yksilo.service.koski.PermissionRequiredException;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@ConditionalOnBean(KoskiOauth2Config.class)
@Slf4j
@RestController
@Tag(name = "integraatiot-koski")
@FeatureRequired(Feature.KOSKI)
@PreAuthorize("hasRole(T(fi.okm.jod.yksilo.config.JodRole).FULL_USER.name())")
public class IntegraatioKoskiController {

  static final String RESULT_PARAM = "koski";
  private static final String API_PATH = "/api/integraatiot/koski/koulutukset";

  private final KoskiOauth2Config koskiConfig;
  private final OAuth2AuthorizedClientRepository authorizedClients;
  private final KoskiService koskiService;

  public IntegraatioKoskiController(
      KoskiOauth2Config koskiConfig,
      OAuth2AuthorizedClientRepository authorizedClients,
      KoskiService koskiService) {
    this.koskiConfig = koskiConfig;
    this.authorizedClients = authorizedClients;
    this.koskiService = koskiService;
  }

  /*
   * OAuth2 authorization code flow callback endpoint. This endpoint gets triggered when the OAuth2
   * authorization code flow is completed upon either fail or success.
   */
  @Hidden
  @GetMapping("/oauth2/response/" + KoskiOauth2Config.REGISTRATION_ID)
  public ResponseEntity<Void> oauth2Response(
      Authentication authentication,
      HttpServletRequest request,
      @AuthenticationPrincipal JodUser jodUser,
      @RequestParam(name = "error", required = false) String error,
      @RequestParam(name = "error_description", required = false) String errorDescription) {

    var callback = takeSavedCallback(request);
    String result = "error";

    if (error != null) {
      if ("access_denied".equalsIgnoreCase(error)) {
        log.atInfo()
            .addMarker(LogMarker.AUDIT)
            .addKeyValue("userId", jodUser.getId())
            .log("User {} canceled Koski authorization", jodUser.getId());
        result = "cancel";
      } else {
        log.warn(
            "Koski OAuth2 authorization failed. Error: {}, description: {}",
            error,
            errorDescription);
      }
    }

    var authorizedClient =
        error == null
            ? authorizedClients.loadAuthorizedClient(
                koskiConfig.getRegistrationId(), authentication, request)
            : null;

    if (authorizedClient != null) {
      log.atInfo()
          .addMarker(LogMarker.AUDIT)
          .addKeyValue("userId", jodUser.getId())
          .log("User {} authorized Koski import", jodUser.getId());
      result = "authorized";
    }

    return redirectToCallback(callback, result);
  }

  /*
   * Koski posts the authorization response (response_mode=form_post). A valid response is
   * processed by the OAuth2AuthorizationCodeGrantFilter, which then redirects to the GET endpoint
   * above. A response reaching this endpoint did not match the pending authorization request
   * (e.g. a stale state after the user started another authorization in another tab), so it is
   * never treated as authorized.
   */
  @Hidden
  @PostMapping("/oauth2/response/" + KoskiOauth2Config.REGISTRATION_ID)
  public ResponseEntity<Void> unmatchedOauth2Response(
      HttpServletRequest request,
      @AuthenticationPrincipal JodUser jodUser,
      @RequestParam(name = "error", required = false) String error) {
    var callback = takeSavedCallback(request);
    if ("access_denied".equalsIgnoreCase(error)) {
      log.atInfo()
          .addMarker(LogMarker.AUDIT)
          .addKeyValue("userId", jodUser.getId())
          .log("User {} canceled Koski authorization", jodUser.getId());
      return redirectToCallback(callback, "cancel");
    }
    log.warn("Koski OAuth2 authorization response did not match a pending authorization request");
    return redirectToCallback(callback, "error");
  }

  private static String takeSavedCallback(HttpServletRequest request) {
    var session = request.getSession();
    var callback = (String) session.getAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey());
    session.removeAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey());
    return callback == null || callback.isBlank() ? request.getContextPath() + "/" : callback;
  }

  private static ResponseEntity<Void> redirectToCallback(String callback, String result) {
    return ResponseEntity.status(HttpStatus.SEE_OTHER)
        .location(
            UriComponentsBuilder.fromPath(callback)
                .queryParam(RESULT_PARAM, result)
                .build()
                .toUri())
        .build();
  }

  @PostMapping(API_PATH)
  @Operation(summary = "Fetch educations from Koski and persist as a verified import task.")
  ResponseEntity<KoskiTehtavaDto> createKoskiTehtava(
      @AuthenticationPrincipal JodUser jodUser,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    var registrationId = koskiConfig.getRegistrationId();
    var authorizedClient =
        authorizedClients.loadAuthorizedClient(registrationId, authentication, request);
    if (authorizedClient == null) {
      throw new PermissionRequiredException(jodUser.getId());
    }
    // The consent is single-use; a new import needs a new one.
    authorizedClients.removeAuthorizedClient(registrationId, authentication, request, response);
    var dto = koskiService.startImport(jodUser, authorizedClient.getAccessToken());
    log.atInfo()
        .addMarker(LogMarker.AUDIT)
        .log("User {} Koski import task {} created", jodUser.getId(), dto.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @GetMapping(API_PATH)
  @Operation(summary = "List the user's unfinished Koski import tasks, newest first.")
  public List<KoskiTehtavaDto> getKeskeneraisetKoskiTehtavat(
      @AuthenticationPrincipal JodUser user) {
    return koskiService.findUnfinished(user);
  }

  @GetMapping(API_PATH + "/{tehtavaId}")
  @Operation(summary = "Get status and content of a Koski import task.")
  public KoskiTehtavaDto getKoskiTehtava(
      @PathVariable UUID tehtavaId, @AuthenticationPrincipal JodUser user) {
    return koskiService.getStatus(user, tehtavaId);
  }

  @PostMapping(API_PATH + "/{tehtavaId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Persist selected Koski educations to the profile.")
  public void saveKoskiTehtava(
      @PathVariable UUID tehtavaId,
      @RequestBody @Valid KoskiTehtavaSaveDto dto,
      @AuthenticationPrincipal JodUser user) {
    var ids = koskiService.save(user, tehtavaId, dto);
    log.atInfo()
        .addMarker(LogMarker.AUDIT)
        .log(
            "User {} saved {} verified Koski koulutuskokonaisuudet from task {}",
            user.getId(),
            ids.size(),
            tehtavaId);
  }

  @DeleteMapping(API_PATH + "/{tehtavaId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a Koski import task.")
  public void deleteKoskiTehtava(
      @PathVariable UUID tehtavaId, @AuthenticationPrincipal JodUser user) {
    koskiService.delete(user, tehtavaId);
  }
}
