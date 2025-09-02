/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.externalapi.v1;

import static fi.okm.jod.yksilo.validation.Limits.SIVUN_MAKSIMI_KOKO;

import fi.okm.jod.yksilo.dto.SivuDto;
import fi.okm.jod.yksilo.externalapi.v1.dto.ExtKoulutusMahdollisuusDto;
import fi.okm.jod.yksilo.externalapi.v1.dto.ExtProfiiliDto;
import fi.okm.jod.yksilo.externalapi.v1.dto.ExtTyoMahdollisuusDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Yksilö-backendin ulkoiset rajapinnat. */
@RestController
@RequestMapping("/external-api/v1")
@Tag(name = "Ulkoinen rajapinta v1", description = "Ulkoinen rajapinta v1")
@RequiredArgsConstructor
public class ExternalApiV1Controller {
  public static final String EXT_API_V1_PATH = "/external-api/v1";
  Logger log = LoggerFactory.getLogger(ExternalApiV1Controller.class);

  private final ExternalApiV1Service service;

  @GetMapping("/tyomahdollisuudet")
  @Operation(
      summary = "Get all työmahdollisuudet paged of by page and size",
      description = "Returns all työmahdollisuudet basic information in JSON-format.")
  public SivuDto<ExtTyoMahdollisuusDto> findTyoMahdollisuudet(
      @RequestParam(required = false, defaultValue = "0") Integer sivu,
      @RequestParam(required = false, defaultValue = "10") Integer koko) {
    Pageable pageable = PageRequest.of(sivu, koko);
    final SivuDto<ExtTyoMahdollisuusDto> tyomahdollisuudet =
        service.findTyomahdollisuudet(pageable);
    log.info("Ext API: Successfully fetched {} tyomahdollisuutta", tyomahdollisuudet.maara());
    return tyomahdollisuudet;
  }

  @GetMapping("/koulutusmahdollisuudet")
  @Operation(
      summary = "Get all koulutusmahdollisuudet paged of by page and size",
      description = "Returns all koulutusmahdollisuudet basic information in JSON-format.")
  public SivuDto<ExtKoulutusMahdollisuusDto> findKoulutusMahdollisuudet(
      @RequestParam(required = false, defaultValue = "0") Integer sivu,
      @RequestParam(required = false, defaultValue = "10") Integer koko) {
    Pageable pageable = PageRequest.of(sivu, koko);
    final SivuDto<ExtKoulutusMahdollisuusDto> koulutusmahdollisuudet =
        service.findKoulutusmahdollisuudet(pageable);
    log.info(
        "Ext API: Successfully fetched {} koulutusmahdollisuutta", koulutusmahdollisuudet.maara());
    return koulutusmahdollisuudet;
  }

  @GetMapping("/profiilit")
  @Operation(
      summary = "Get all profiilit paged of by page and size",
      description = "Returns all profiilit basic information in JSON-format.")
  public SivuDto<ExtProfiiliDto> findProfiilit(
      @RequestParam(required = false, defaultValue = "0") Integer sivu,
      @RequestParam(required = false, defaultValue = "10")
          @Max(value = SIVUN_MAKSIMI_KOKO, message = "Sivun maksimikoko on 1000")
          Integer koko,
      @Parameter(
              description = "Only get profiles modified after this timestamp",
              example = "2025-07-03T09:00:00Z")
          @RequestParam(name = "muokattuJalkeen", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
          Instant modifiedAfter) {
    Pageable pageable = PageRequest.of(sivu, koko);
    final SivuDto<ExtProfiiliDto> yksilot = service.findYksilot(modifiedAfter, pageable);
    log.info("Ext API:Successfully fetched {} profiilia", yksilot.maara());
    return yksilot;
  }
}
