/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.ehdotus;

import fi.okm.jod.yksilo.dto.NormalizedString;
import fi.okm.jod.yksilo.service.ehdotus.OsaamisetEhdotusService;
import fi.okm.jod.yksilo.service.ehdotus.OsaamisetEhdotusService.Ehdotus;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mock controller */
@RestController
@RequestMapping(path = "/api/ehdotus/osaamiset")
@Slf4j
@Tag(name = "ehdotus", description = "Ehdotus (POC)")
@RequiredArgsConstructor
class OsaamisetEhdotusController {

  private final OsaamisetEhdotusService service;

  @PostMapping
  @Timed
  public ResponseEntity<List<Ehdotus>> createEhdotus(@RequestBody @Valid Taidot taidot) {
    return ResponseEntity.ok(service.createEhdotus(taidot.kuvaus().value()));
  }

  public record Taidot(@NotNull @Size(min = 3, max = 10_000) NormalizedString kuvaus) {}
}
