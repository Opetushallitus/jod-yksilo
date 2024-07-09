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
import fi.okm.jod.yksilo.dto.profiili.KategoriaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKategoriaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusUpdateResultDto;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiili/koulutukset")
@RequiredArgsConstructor
@Tag(name = "profiili")
class KoulutusController {
  private final KoulutusService service;

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get all koulutukset and kategoriat of the user")
  List<KoulutusKategoriaDto> find(@AuthenticationPrincipal JodUser user) {
    return service.findAll(user);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary =
          "Creates a set of Koulutus associated with an optional Kategoria, creating the Kategoria if necessary")
  KoulutusUpdateResultDto add(
      @RequestBody @Validated(Add.class) KoulutusKategoriaDto dto,
      @AuthenticationPrincipal JodUser user) {
    return service.upsert(user, dto.kategoria(), dto.koulutukset());
  }

  @PutMapping
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Updates a set of Koulutus",
      description = "If an existing Koulutus in a Kategoria is omitted, it will be removed.")
  KoulutusUpdateResultDto update(
      @RequestBody @Valid KoulutusKategoriaDto dto, @AuthenticationPrincipal JodUser user) {
    return service.merge(user, dto.kategoria(), dto.koulutukset());
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteAll(@RequestParam Set<UUID> ids, @AuthenticationPrincipal JodUser user) {
    service.delete(user, ids);
  }

  @PutMapping(path = "/{id}")
  @ResponseStatus(HttpStatus.OK)
  void updateKoulutus(
      @PathVariable UUID id,
      @RequestBody @Valid KoulutusDto dto,
      @AuthenticationPrincipal JodUser user) {
    if (dto.id() == null || !id.equals(dto.id())) {
      throw new IllegalArgumentException("Invalid identifier");
    }
    service.update(user, dto);
  }

  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  KoulutusDto get(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    return service.find(user, id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    service.delete(user, Set.of(id));
  }

  @GetMapping("/kategoriat")
  List<KategoriaDto> getKategoriat(@AuthenticationPrincipal JodUser user) {
    return service.getKategoriat(user);
  }

  @PutMapping(path = "/kategoriat/{id}")
  @ResponseStatus(HttpStatus.OK)
  void updateKategoria(
      @PathVariable UUID id,
      @RequestBody @Valid KategoriaDto dto,
      @AuthenticationPrincipal JodUser user) {
    if (dto.id() == null || !id.equals(dto.id())) {
      throw new IllegalArgumentException("Invalid identifier");
    }
    service.updateKategoria(user, dto);
  }
}
