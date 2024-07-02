/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.service.KuvaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kuvat")
@Tag(name = "kuvat")
public class KuvaController {
  private final KuvaService service;

  @GetMapping("/{id}")
  public ResponseEntity<byte[]> get(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    var kuva = service.find(user, id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(kuva.getTyyppi()))
        .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
        .body(kuva.getData());
  }
}
