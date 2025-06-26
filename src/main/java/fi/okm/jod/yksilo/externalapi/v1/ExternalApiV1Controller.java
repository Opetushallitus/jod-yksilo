/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.externalapi.v1;

import fi.okm.jod.yksilo.dto.SivuDto;
import fi.okm.jod.yksilo.externalapi.v1.dto.ExtTyoMahdollisuusDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Yksilö-backendin ulkoiset rajapinnat */
@RestController
@RequestMapping("/external-api/v1")
@RequiredArgsConstructor
public class ExternalApiV1Controller {

  private final ExternalApiV1Service service;

  @GetMapping("/tyomahdollisuudet")
  @Operation(
      summary = "Get all työmahdollisuudet paged of by page and size",
      description = "Returns all työmahdollisuudet basic information in JSON-format.")
  public SivuDto<ExtTyoMahdollisuusDto> findTyoMahdollisuudet(
      @RequestParam(required = false, defaultValue = "0") Integer sivu,
      @RequestParam(required = false, defaultValue = "10") Integer koko) {
    Pageable pageable = PageRequest.of(sivu, koko);
    return service.findTyoMahdollisuudet(pageable);
  }
}
