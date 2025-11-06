/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import fi.okm.jod.yksilo.config.feature.Feature;
import fi.okm.jod.yksilo.config.feature.FeatureRequired;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.TavoiteDto;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import fi.okm.jod.yksilo.service.profiili.TavoiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiili/tavoitteet")
@RequiredArgsConstructor
@Tag(name = "profiili/tavoitteet")
@FeatureRequired(Feature.TAVOITE)
class TavoiteController {
  private final TavoiteService service;

  @GetMapping
  @Operation(summary = "Gets all tavoitteet")
  List<TavoiteDto> findAll(@AuthenticationPrincipal JodUser user) {
    final List<TavoiteDto> all = service.findAll(user);
    return all;
  }

  @PostMapping
  @Operation(summary = "Adds a new tavoite")
  @ResponseStatus(HttpStatus.CREATED)
  UUID add(
      @AuthenticationPrincipal JodUser user, @Validated(Add.class) @RequestBody TavoiteDto dto) {
    return service.add(user, dto);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Updates a tavoite")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void update(
      @AuthenticationPrincipal JodUser user,
      @PathVariable UUID id,
      @Valid @RequestBody TavoiteDto dto) {
    if (dto.id() == null || !id.equals(dto.id())) {
      throw new IllegalArgumentException("Invalid identifier");
    }
    service.update(user, dto);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Deletes a tavoite")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    service.delete(user, id);
  }
}
