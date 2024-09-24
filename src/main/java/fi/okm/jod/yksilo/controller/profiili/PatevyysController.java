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
import fi.okm.jod.yksilo.dto.profiili.PatevyysDto;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import fi.okm.jod.yksilo.service.profiili.PatevyysService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/profiili/vapaa-ajan-toiminnot/{id}/patevyydet")
@RequiredArgsConstructor
@Tag(name = "profiili/vapaa-ajan-toiminnot")
class PatevyysController {
  private final PatevyysService service;

  @GetMapping
  @Operation(summary = "Gets all patevyydet of the vapaa-ajan toiminto")
  List<PatevyysDto> findAll(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    return service.findAll(user, id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Adds a new patevyys to the vapaa-ajan toiminto")
  ResponseEntity<IdDto<UUID>> add(
      @PathVariable UUID id,
      @Validated({Add.class}) @RequestBody PatevyysDto dto,
      @AuthenticationPrincipal JodUser user) {

    var patevyysId = service.add(user, id, dto);
    var location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .host(null)
            .path("/{patevyysId}")
            .buildAndExpand(patevyysId)
            .toUri();
    return ResponseEntity.created(location).body(new IdDto<>(patevyysId));
  }

  @GetMapping("/{patevyysId}")
  @Operation(summary = "Gets a patevyys of the vapaa-ajan toiminto")
  PatevyysDto get(
      @PathVariable UUID id, @PathVariable UUID patevyysId, @AuthenticationPrincipal JodUser user) {
    return service.get(user, id, patevyysId);
  }

  @PutMapping("/{patevyysId}")
  @Operation(summary = "Updates a patevyys of the vapaa-ajan toiminto (including osaamiset)")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void update(
      @PathVariable UUID id,
      @PathVariable UUID patevyysId,
      @Valid @RequestBody PatevyysDto dto,
      @AuthenticationPrincipal JodUser user) {

    if (dto.id() == null || !patevyysId.equals(dto.id())) {
      throw new IllegalArgumentException("Invalid identifier");
    }
    service.update(user, id, dto);
  }

  @DeleteMapping("/{patevyysId}")
  @Operation(
      summary =
          "Deletes a patevyys of the vapaa-ajan toiminto (including all osaamiset)."
              + " If the toiminto becomes empty, it will also be deleted.")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(
      @PathVariable UUID id, @PathVariable UUID patevyysId, @AuthenticationPrincipal JodUser user) {
    service.delete(user, id, patevyysId);
  }
}
