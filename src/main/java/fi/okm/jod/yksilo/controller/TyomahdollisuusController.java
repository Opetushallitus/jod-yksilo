/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import fi.okm.jod.yksilo.dto.SivuDto;
import fi.okm.jod.yksilo.dto.TyomahdollisuusDto;
import fi.okm.jod.yksilo.service.TyomahdollisuusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tyomahdollisuudet")
@RequiredArgsConstructor
@Tag(name = "tyomahdollisuudet", description = "Työmahdollisuuksien listaus")
public class TyomahdollisuusController {
  private final TyomahdollisuusService tyomahdollisuusService;

  @GetMapping
  @Operation(
      summary = "Hae työmahdollisuudet",
      description = "Palauttaa työpaikkailmoitukset JSON-muodossa.")
  public SivuDto<TyomahdollisuusDto> findAll(
      @RequestParam(required = false, defaultValue = "0") Integer sivu,
      @RequestParam(required = false, defaultValue = "10") Integer koko) {
    Pageable pageable = PageRequest.of(sivu, koko);
    return new SivuDto<>(tyomahdollisuusService.findAll(pageable));
  }
}
