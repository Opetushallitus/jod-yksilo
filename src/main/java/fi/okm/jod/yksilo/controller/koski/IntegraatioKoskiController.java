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
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaDto;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaSaveDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.service.koski.KoskiOauth2Service;
import fi.okm.jod.yksilo.service.koski.KoskiService;
import fi.okm.jod.yksilo.validation.Limits;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnBean(KoskiOauth2Config.class)
@Slf4j
@RestController
@RequestMapping("/api/integraatiot/koski")
@Tag(name = "integraatiot-koski")
@FeatureRequired(Feature.KOSKI)
@PreAuthorize("hasRole(T(fi.okm.jod.yksilo.config.JodRole).FULL_USER.name())")
public class IntegraatioKoskiController {

  private final KoskiOauth2Service koskiOauth2Service;
  private final KoskiService koskiService;

  public IntegraatioKoskiController(
      KoskiOauth2Service koskiOauth2Service, KoskiService koskiService) {
    this.koskiOauth2Service = koskiOauth2Service;
    this.koskiService = koskiService;
  }

  @PostMapping("/koulutukset")
  @Operation(summary = "Fetch educations from Koski and persist as a verified import task.")
  ResponseEntity<KoskiTehtavaDto> createKoskiTehtava(
      @AuthenticationPrincipal JodUser jodUser,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    var dataInJson = koskiOauth2Service.fetchKoskiData(jodUser, authentication, request, response);
    var koulutukset = koskiService.mapKoulutusKokonaisuudet(dataInJson);
    var dto = koskiService.submit(jodUser, koulutukset);
    log.atInfo()
        .addMarker(LogMarker.AUDIT)
        .log("User {} Koski import task {} created", jodUser.getId(), dto.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @GetMapping("/koulutukset/{tehtavaId}")
  @Operation(summary = "Get status and content of a Koski import task.")
  public KoskiTehtavaDto getKoskiTehtava(
      @PathVariable UUID tehtavaId, @AuthenticationPrincipal JodUser user) {
    return koskiService.getStatus(user, tehtavaId);
  }

  @PostMapping("/koulutukset/{tehtavaId}")
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

  @DeleteMapping("/koulutukset/{tehtavaId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a Koski import task.")
  public void deleteKoskiTehtava(
      @PathVariable UUID tehtavaId, @AuthenticationPrincipal JodUser user) {
    koskiService.delete(user, tehtavaId);
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
