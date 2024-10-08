/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import fi.okm.jod.yksilo.dto.KoulutusmahdollisuusDto;
import fi.okm.jod.yksilo.dto.KoulutusmahdollisuusFullDto;
import fi.okm.jod.yksilo.dto.SivuDto;
import fi.okm.jod.yksilo.service.KoulutusmahdollisuusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/koulutusmahdollisuudet")
@RequiredArgsConstructor
@Tag(name = "koulutusmahdollisuudet")
public class KoulutusmahdollisuusController {
  private final KoulutusmahdollisuusService koulutusmahdollisuusService;

  @GetMapping
  public SivuDto<KoulutusmahdollisuusDto> findAll(
      @RequestParam(required = false, defaultValue = "0") Integer sivu,
      @RequestParam(required = false, defaultValue = "10") Integer koko,
      @RequestParam(required = false) Set<UUID> id) {
    if (id == null) {
      Pageable pageable = PageRequest.of(sivu, koko);
      return new SivuDto<>(koulutusmahdollisuusService.findAll(pageable));
    }

    // Return paged also when requested by IDs
    return new SivuDto<>(new PageImpl<>(koulutusmahdollisuusService.findByIds(id)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get full information content of a koulutusmahdollisuus")
  public ResponseEntity<KoulutusmahdollisuusFullDto> findById(@PathVariable UUID id) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
        .body(koulutusmahdollisuusService.get(id));
  }
}
