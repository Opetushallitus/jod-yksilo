/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.PolunVaiheDto;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import fi.okm.jod.yksilo.service.profiili.PolunVaiheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiili/paamaarat/{id}/suunnitelmat/{suunnitelmaId}/vaiheet")
@RequiredArgsConstructor
@Tag(name = "profiili/paamaarat")
public class PolunVaiheController {
  private final PolunVaiheService service;

  @PostMapping
  @Operation(summary = "Adds a new vaihe to the suunnitelma")
  @ResponseStatus(HttpStatus.CREATED)
  public UUID add(
      @AuthenticationPrincipal JodUser user,
      @PathVariable UUID id,
      @PathVariable UUID suunnitelmaId,
      @Validated(Add.class) @RequestBody PolunVaiheDto dto) {
    return service.add(user, id, suunnitelmaId, dto);
  }

  @PutMapping("/{vaiheId}")
  @Operation(summary = "Updates a vaihe of the suunnitelma")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(
      @AuthenticationPrincipal JodUser user,
      @PathVariable UUID id,
      @PathVariable UUID suunnitelmaId,
      @PathVariable UUID vaiheId,
      @Valid @RequestBody PolunVaiheDto dto) {
    if (dto.id() == null || !vaiheId.equals(dto.id())) {
      throw new IllegalArgumentException("Invalid identifier");
    }
    service.update(user, id, suunnitelmaId, dto);
  }

  @DeleteMapping("/{vaiheId}")
  @Operation(summary = "Deletes a vaihe of the suunnitelma")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @AuthenticationPrincipal JodUser user,
      @PathVariable UUID id,
      @PathVariable UUID suunnitelmaId,
      @PathVariable UUID vaiheId) {
    service.delete(user, id, suunnitelmaId, vaiheId);
  }
}
