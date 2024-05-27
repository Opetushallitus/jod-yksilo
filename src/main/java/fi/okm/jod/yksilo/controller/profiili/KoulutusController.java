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
import fi.okm.jod.yksilo.dto.IdDto;
import fi.okm.jod.yksilo.dto.profiili.KategoriaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKategoriaDto;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
  List<KoulutusKategoriaDto> find(
      @AuthenticationPrincipal JodUser user, @RequestParam(required = false) String kategoriaId) {
    return switch (kategoriaId) {
      case null -> service.findAll(user);
      case "null" -> service.findAll(user, null);
      default -> service.findAll(user, UUID.fromString(kategoriaId));
    };
  }

  @PutMapping
  ResponseEntity<IdDto<UUID>> update(
      @RequestBody @Valid KoulutusKategoriaDto dto, @AuthenticationPrincipal JodUser user) {
    var id = service.merge(user, dto.kategoria(), dto.koulutukset());
    return ResponseEntity.ok(new IdDto<>(id));
  }

  @PatchMapping
  ResponseEntity<IdDto<UUID>> partialUpdate(
      @RequestBody @Valid KoulutusKategoriaDto dto, @AuthenticationPrincipal JodUser user) {
    var id = service.upsert(user, dto.kategoria(), dto.koulutukset());
    return ResponseEntity.ok(new IdDto<>(id));
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
}
