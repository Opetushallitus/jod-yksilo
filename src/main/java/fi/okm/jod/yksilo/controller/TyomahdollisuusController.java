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
import fi.okm.jod.yksilo.dto.tyomahdollisuus.TyomahdollisuusDto;
import fi.okm.jod.yksilo.dto.tyomahdollisuus.TyomahdollisuusFullDto;
import fi.okm.jod.yksilo.service.TyomahdollisuusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tyomahdollisuudet")
@RequiredArgsConstructor
@Tag(name = "tyomahdollisuudet", description = "Työmahdollisuus listing")
public class TyomahdollisuusController {
  private final TyomahdollisuusService tyomahdollisuusService;

  @GetMapping
  @Operation(
      summary = "Get all työmahdollisuudet paged of by page and size or set of ids",
      description = "Returns all työmahdollisuudet basic information in JSON-format.")
  public SivuDto<TyomahdollisuusDto> findAll(
      @RequestParam(required = false, defaultValue = "0") @Min(0) int sivu,
      @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(1000) int koko,
      @RequestParam(required = false) Set<UUID> id) {
    if (id == null) {
      Pageable pageable = PageRequest.of(sivu, koko);
      return new SivuDto<>(tyomahdollisuusService.findAll(pageable));
    }

    // Return paged also when requested by IDs
    return new SivuDto<>(new PageImpl<>(tyomahdollisuusService.findByIds(id)));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get full information content of single työmahdollisuus",
      description = "Returns one työmahdollisuus full content by id")
  public ResponseEntity<TyomahdollisuusFullDto> findById(@PathVariable UUID id) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
        .body(tyomahdollisuusService.get(id));
  }
}
