/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import static fi.okm.jod.yksilo.controller.Etags.weakEtagOf;

import fi.okm.jod.yksilo.dto.AmmattiDto;
import fi.okm.jod.yksilo.dto.SivuDto;
import fi.okm.jod.yksilo.service.AmmattiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequestMapping("/api/ammatit")
@RequiredArgsConstructor
@Tag(name = "ammatit", description = "Ammattien (ESCO) listaus")
class AmmattiController {
  private final AmmattiService ammatit;

  @GetMapping
  ResponseEntity<SivuDto<AmmattiDto>> find(
      WebRequest request,
      @RequestParam(required = false, defaultValue = "0") @Min(0) int sivu,
      @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(1000) int koko,
      @RequestParam(required = false) Set<URI> uri) {

    var etag = weakEtagOf(ammatit.currentVersion());
    SivuDto<AmmattiDto> body;

    if (request.checkNotModified(etag)) {
      body = null;
    } else {
      var result = (uri == null) ? ammatit.findAll(sivu, koko) : ammatit.findBy(sivu, koko, uri);
      body = result.payload();
      etag = weakEtagOf(result.version());
    }

    return ResponseEntity.ok()
        .eTag(etag)
        .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).mustRevalidate().cachePublic())
        .body(body);
  }
}
