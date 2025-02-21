/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.koski;

import com.codahale.metrics.Clock;
import fi.okm.jod.yksilo.config.koski.KoskiOAuth2Config;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.service.koski.KoskiOAuth2Service;
import fi.okm.jod.yksilo.service.koski.KoskiService;
import fi.okm.jod.yksilo.service.koski.PermissionRequiredException;
import fi.okm.jod.yksilo.service.koski.WrongPersonException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnBean(KoskiOAuth2Config.class)
@Slf4j
@RestController
@RequestMapping("/api/integraatiot/koski")
@Tag(name = "integraatiot-koski")
public class IntegraatioKoskiController {

  private final KoskiOAuth2Service koskiOAuth2Service;
  private final KoskiService koskiService;

  public IntegraatioKoskiController(
      KoskiOAuth2Service koskiOAuth2Service, KoskiService koskiService) {
    this.koskiOAuth2Service = koskiOAuth2Service;
    this.koskiService = koskiService;
  }

  @ConditionalOnBean(KoskiOAuth2Config.class)
  @GetMapping("/koulutukset")
  @Operation(summary = "Get user's education's histories from Koski's opintopolku.")
  ResponseEntity<List<KoulutusDto>> getEducationsDataFromKoski(
      @AuthenticationPrincipal JodUser jodUser,
      Authentication oauth2User,
      HttpServletRequest request,
      HttpServletResponse response) {
    var authorizedClient = koskiOAuth2Service.getAuthorizedClient(oauth2User, request);
    if (authorizedClient == null) {
      throw new PermissionRequiredException(jodUser.getId());
    }

    try {
      var clock = Clock.defaultClock();
      long startTime = clock.getTime();
      var dataInJson = koskiOAuth2Service.fetchDataFromResourceServer(authorizedClient);
      long duration = clock.getTime() - startTime;
      if (duration > 1_000) {
        log.warn(
            "Fetching data from Koski's opintopolku took {} ms, which longer than expected (1s).",
            duration);
      }
      koskiOAuth2Service.checkPersonIdMatches(jodUser, dataInJson);

      var educationHistories = koskiService.getKoulutusData(dataInJson, null);
      return ResponseEntity.ok(educationHistories);

    } catch (WrongPersonException e) {
      koskiOAuth2Service.unauthorize(oauth2User, request, response);
      throw e;
    }
  }
}
