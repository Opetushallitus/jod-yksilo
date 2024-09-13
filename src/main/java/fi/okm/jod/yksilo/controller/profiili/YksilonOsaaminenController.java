/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
import fi.okm.jod.yksilo.dto.profiili.YksilonOsaaminenDto;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiili/osaamiset")
@RequiredArgsConstructor
@Tag(name = "profiili/osaamiset")
class YksilonOsaaminenController {
  private final YksilonOsaaminenService service;

  @GetMapping
  @Operation(summary = "Finds all Osaaminen, optionally filtered by Tyyppi and/or LahdeId")
  List<YksilonOsaaminenDto> find(
      @AuthenticationPrincipal JodUser user,
      @RequestParam(required = false) OsaamisenLahdeTyyppi tyyppi,
      @RequestParam(required = false) UUID lahdeId) {
    return service.findAll(user, tyyppi, lahdeId);
  }

  @DeleteMapping
  @Operation(summary = "Deletes one or more Yksilo's Osaaminen")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(
      @RequestParam @NotEmpty @Size(max = 1000) Set<UUID> ids,
      @AuthenticationPrincipal JodUser user) {
    service.delete(user, ids);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Gets a Yksilo's Osaaminen")
  YksilonOsaaminenDto get(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    return service.get(user, id);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Deletes a Yksilo's Osaaminen")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    service.delete(user, Set.of(id));
  }
}
