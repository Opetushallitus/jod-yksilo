/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.koski;

import fi.okm.jod.yksilo.config.feature.Feature;
import fi.okm.jod.yksilo.config.feature.FeatureRequired;
import fi.okm.jod.yksilo.config.koski.KoskiOauth2Config;
import fi.okm.jod.yksilo.config.logging.LogMarker;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.service.koski.KoskiOauth2Service;
import fi.okm.jod.yksilo.service.koski.KoskiService;
import fi.okm.jod.yksilo.service.koski.PermissionRequiredException;
import fi.okm.jod.yksilo.service.koski.WrongPersonException;
import fi.okm.jod.yksilo.validation.Limits;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnBean(KoskiOauth2Config.class)
@Slf4j
@RestController
@RequestMapping("/api/integraatiot/koski")
@Tag(name = "integraatiot-koski")
@FeatureRequired(Feature.KOSKI)
public class IntegraatioKoskiController {

  private final KoskiOauth2Service koskiOauth2Service;
  private final KoskiService koskiService;

  public IntegraatioKoskiController(
      KoskiOauth2Service koskiOauth2Service, KoskiService koskiService) {
    this.koskiOauth2Service = koskiOauth2Service;
    this.koskiService = koskiService;
  }

  @GetMapping("/koulutukset")
  @Operation(summary = "Get user's education's histories from Koski's opintopolku.")
  @Timed
  ResponseEntity<List<KoulutusDto>> getEducationsDataFromKoski(
      @AuthenticationPrincipal JodUser jodUser,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    var authorizedClient = koskiOauth2Service.getAuthorizedClient(authentication, request);
    if (authorizedClient == null) {
      throw new PermissionRequiredException(jodUser.getId());
    }
    koskiOauth2Service.unauthorize(authentication, request, response);

    try {
      var dataInJson = koskiOauth2Service.fetchDataFromResourceServer(jodUser, authorizedClient);
      var educationHistories = koskiService.mapKoulutusData(dataInJson);
      log.atInfo()
          .addMarker(LogMarker.AUDIT)
          .log("User {} education history imported", jodUser.getId());
      return ResponseEntity.ok(educationHistories);
    } catch (WrongPersonException e) {
      log.atWarn()
          .addMarker(LogMarker.AUDIT)
          .log("User {} tried to access another person's Koski data.", jodUser.getId());
      throw e;
    }
  }

  @GetMapping("/osaamiset/tunnistus")
  public ResponseEntity<List<KoulutusDto>> osaamisenTunnistusStatusQuery(
      @AuthenticationPrincipal JodUser user,
      @RequestParam("ids")
          @Parameter(description = "Koulutus ids")
          @Size(min = 1, max = Limits.KOULUTUSKOKONAISUUS)
          List<UUID> uuids) {
    return ResponseEntity.ok(koskiService.getOsaamisetIdentified(user, uuids));
  }
}
