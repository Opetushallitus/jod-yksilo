/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.koski;

import com.fasterxml.jackson.databind.JsonNode;
import fi.okm.jod.yksilo.config.koski.KoskiOAuth2Config;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.service.koski.KoskiOAuth2Service;
import fi.okm.jod.yksilo.service.koski.KoskiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

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
      throw new AccessDeniedException("Permission was not given or it is missing.");
    }

    JsonNode dataInJson;
    try {
      dataInJson = koskiOAuth2Service.fetchDataFromResourceServer(authorizedClient);
      checkPersonIdMatches(jodUser, oauth2User, getPersonId(dataInJson));

      var educationHistories = koskiService.getKoulutusData(dataInJson, null);
      return ResponseEntity.ok(educationHistories);

    } catch (HttpClientErrorException e) {
      log.debug(
          "Fail to get data from Koski resource server. Http error code: {}", e.getStatusCode(), e);
      if (e.getStatusCode() == HttpStatus.UNAUTHORIZED
          || e.getStatusCode() == HttpStatus.FORBIDDEN) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    } catch (PersonIdNotMatchException e) {
      log.warn("Person ID did NOT match. JOD user ({}) != OAuth2 user.", jodUser.getId());
      koskiOAuth2Service.logout(oauth2User, request, response);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
  }

  private void checkPersonIdMatches(
      JodUser jodUser, Authentication oauth2User, String oauth2PersonId)
      throws PersonIdNotMatchException {
    // if (true) return false; //Bypass for development purpose.
    var jodUserPersonId = jodUser.getPersonId();
    if (!StringUtils.endsWithIgnoreCase(jodUserPersonId, oauth2PersonId)) {
      throw new PersonIdNotMatchException(jodUser, oauth2User);
    }
  }

  @Getter
  @AllArgsConstructor
  private static class PersonIdNotMatchException extends Exception {
    private final JodUser jodUser;
    private final Authentication oauth2User;
  }

  public String getPersonId(JsonNode jsonData) {
    return jsonData.get("henkilö").get("hetu").asText();
  }
}
