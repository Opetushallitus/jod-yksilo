/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.tmt;

import fi.okm.jod.yksilo.config.tmt.InvalidTokenException;
import fi.okm.jod.yksilo.config.tmt.TmtAuthorizationRepository;
import fi.okm.jod.yksilo.config.tmt.TmtConfiguration;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.service.tmt.TmtExportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/integraatiot/tmt")
@RequiredArgsConstructor
@Tag(name = "integraatiot/tmt")
@Slf4j
@ConditionalOnProperty(name = "jod.tmt.enabled", havingValue = "true")
public class TmtExportController {

  public static final String EXPORT_ID = "exportId";
  public static final String ERROR_PARAM = "error";
  private final TmtAuthorizationRepository authorizationRepository;
  private final TmtConfiguration config;
  private final TmtExportService service;

  @GetMapping("/vienti/auktorisointi")
  ResponseEntity<Void> authorize(
      @AuthenticationPrincipal JodUser user, @RequestParam URI callback) {

    var authorizationRequest = authorizationRepository.createAuthorizationRequest(callback);
    if (!service.canExport(user)) {
      return ResponseEntity.status(HttpStatus.SEE_OTHER)
          .location(
              UriComponentsBuilder.fromPath(authorizationRequest.callback())
                  .queryParam(ERROR_PARAM, "export_not_allowed")
                  .build()
                  .toUri())
          .build();
    }

    var responseUri =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .query(null)
            .buildAndExpand(authorizationRequest.id());
    var redirectUri =
        UriComponentsBuilder.fromUri(config.getAuthorizationUrl())
            .queryParam("redirectUrl", responseUri.toString())
            .build()
            .toUri();
    log.atInfo()
        .addKeyValue(EXPORT_ID, authorizationRequest.id())
        .log("Requesting authorization for user {} profile export", user.getId());
    return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
  }

  @GetMapping("/vienti/auktorisointi/{id}")
  ResponseEntity<Void> response(
      @AuthenticationPrincipal JodUser user,
      @PathVariable UUID id,
      @RequestParam(required = false) @Valid @Size(max = 4096) String token,
      HttpServletRequest request) {
    var authorizationRequest = authorizationRepository.getAuthorizationRequest(id);
    if (token != null && authorizationRequest != null) {
      try {
        authorizationRepository.clearAuthorizationRequest();
        authorizationRepository.saveAccessToken(authorizationRequest, token);
        log.atInfo()
            .addKeyValue(EXPORT_ID, authorizationRequest.id())
            .log("Received TMT authorization for user {} profile export", user.getId());
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
            .location(
                UriComponentsBuilder.fromPath(authorizationRequest.callback())
                    .queryParam(EXPORT_ID, authorizationRequest.id())
                    .build()
                    .toUri())
            .build();
      } catch (InvalidTokenException e) {
        log.atError()
            .addKeyValue(EXPORT_ID, authorizationRequest.id())
            .log("Received invalid access token for user {}", user.getId(), e);
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
            .location(
                UriComponentsBuilder.fromPath(authorizationRequest.callback())
                    .queryParam(ERROR_PARAM, "authorization_failed")
                    .build()
                    .toUri())
            .build();
      }
    } else if (authorizationRequest != null) {
      authorizationRepository.clearAuthorizationRequest();
      // Authorization failed or was interrupted
      return ResponseEntity.status(HttpStatus.SEE_OTHER)
          .location(
              UriComponentsBuilder.fromPath(authorizationRequest.callback())
                  .queryParam(ERROR_PARAM, "authorization_failed")
                  .build()
                  .toUri())
          .build();
    } else {
      log.error("Valid authorization not found");
      return ResponseEntity.status(HttpStatus.SEE_OTHER)
          .location(URI.create(request.getContextPath()))
          .build();
    }
  }

  @PostMapping("/vienti/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @SuppressWarnings("try")
  void export(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    var accessToken = authorizationRepository.getAccessToken(id);
    if (accessToken != null) {
      authorizationRepository.clearAccessToken();
      try (var ignored = MDC.putCloseable(EXPORT_ID, accessToken.id().toString())) {
        log.atInfo().log("Exporting profile for user {}", user.getId());
        service.export(user, accessToken);
      }
    } else {
      log.error("Access token not found");
      throw new IllegalArgumentException("Access token not found");
    }
  }
}
