/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import static fi.okm.jod.yksilo.controller.ETags.weakETagOf;

import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.dto.SivuDto;
import fi.okm.jod.yksilo.service.OsaaminenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
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
@RequestMapping("/api/osaamiset")
@RequiredArgsConstructor
@Tag(name = "osaamiset", description = "Osaamisten (ESCO) listaus")
class OsaaminenController {
  private final OsaaminenService osaamiset;

  @GetMapping
  ResponseEntity<SivuDto<OsaaminenDto>> find(
      WebRequest request,
      @RequestParam(required = false, defaultValue = "0") int sivu,
      @RequestParam(required = false, defaultValue = "10") @Max(1000) int koko,
      @RequestParam(required = false) Set<URI> uri) {

    var etag = weakETagOf(osaamiset.currentVersion());
    SivuDto<OsaaminenDto> body;

    if (request.checkNotModified(etag)) {
      body = null;
    } else {
      var result =
          (uri == null) ? osaamiset.findAll(sivu, koko) : osaamiset.findBy(sivu, koko, uri);
      body = result.payload();
      etag = weakETagOf(result.version());
    }

    return ResponseEntity.ok()
        .eTag(etag)
        .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).mustRevalidate().cachePublic())
        .body(body);
  }
}
