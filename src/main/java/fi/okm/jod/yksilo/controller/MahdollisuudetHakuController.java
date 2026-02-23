/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import fi.okm.jod.yksilo.config.feature.Feature;
import fi.okm.jod.yksilo.config.feature.FeatureRequired;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.MahdollisuusDto;
import fi.okm.jod.yksilo.service.MahdollisuudetSearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.SequencedCollection;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/mahdollisuudet/haku")
@Slf4j
@Tag(name = "mahdollisuudet/haku", description = "Mahdollisuudet search API")
@RequiredArgsConstructor
@FeatureRequired(Feature.MAHDOLLISUUDET_HAKU)
public class MahdollisuudetHakuController {
  private final MahdollisuudetSearchService searchService;

  @GetMapping
  public ResponseEntity<SequencedCollection<MahdollisuusDto>> search(
      @RequestParam(defaultValue = "fi") Kieli kieli,
      @RequestParam @NotEmpty @Size(min = 3, max = 400) String teksti) {

    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS))
        .body(searchService.search(kieli, teksti));
  }
}
